package com.cjx.airplayjavademo.player;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;


import com.cjx.airplayjavademo.model.PCMPacket;
import com.cjx.airplayjavademo.tools.LogRepository;
import com.github.serezhka.jap2lib.rtsp.AudioStreamInfo;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AudioPlayer extends Thread {

    private static final String TAG = "AudioPlayer";
    private static final int QUEUE_CAPACITY = 500;
    private static final int LOG_FIRST_PACKETS = 5;
    private static final int LOG_EVERY_PACKETS = 100;

    private AudioTrack mTrack;
    private final int mChannel;
    private final int mSampleRate;
    private final int mAudioFormat;
    private final int frameSizeBytes;
    private final int bufferSizeBytes;
    private volatile boolean isStopThread = false;
    private volatile boolean audioDisabled = false;
    private int queuedPackets;
    private int droppedPackets;
    private int writtenPackets;
    private long writtenBytes;
    private final BlockingQueue<PCMPacket> packets = new LinkedBlockingQueue<PCMPacket>(QUEUE_CAPACITY);

    public static AudioPlayer createForStreamInfo(AudioStreamInfo audioInfo) {
        AudioConfig config = AudioConfig.from(audioInfo);
        if (config == null) {
            LogRepository.INSTANCE.addLog(TAG, "Audio disabled: unsupported stream " + audioInfo, 'W');
            return null;
        }

        try {
            return new AudioPlayer(config);
        } catch (Exception e) {
            Log.e(TAG, "createForStreamInfo: failed to initialize AudioTrack", e);
            LogRepository.INSTANCE.addLog(TAG, "Audio disabled: failed to initialize AudioTrack: " +
                    e.getMessage(), 'E');
            return null;
        }
    }

    private AudioPlayer(AudioConfig config) {
        mSampleRate = config.sampleRate;
        mChannel = config.channelConfig;
        mAudioFormat = config.audioFormat;
        frameSizeBytes = config.frameSizeBytes;

        int minBufferSize = AudioTrack.getMinBufferSize(mSampleRate, mChannel, mAudioFormat);
        if (minBufferSize <= 0) {
            throw new IllegalStateException("Invalid AudioTrack min buffer size: " + minBufferSize);
        }
        bufferSizeBytes = Math.max(minBufferSize * 2, frameSizeBytes * mSampleRate / 10);

        LogRepository.INSTANCE.addLog(TAG, "Initializing AudioTrack sampleRate=" + mSampleRate +
                " channels=" + config.channels +
                " encoding=PCM_16BIT" +
                " frameSize=" + frameSizeBytes +
                " minBuffer=" + minBufferSize +
                " buffer=" + bufferSizeBytes, 'I');

        mTrack = new AudioTrack(AudioManager.STREAM_MUSIC, mSampleRate, mChannel, mAudioFormat,
                bufferSizeBytes, AudioTrack.MODE_STREAM);
        if (mTrack.getState() != AudioTrack.STATE_INITIALIZED) {
            int state = mTrack.getState();
            releaseTrack();
            throw new IllegalStateException("AudioTrack state is not initialized: " + state);
        }
        try {
            mTrack.play();
        } catch (Exception e) {
            releaseTrack();
            throw e;
        }
    }

    public void addPacker(PCMPacket pcmPacket) {
        if (audioDisabled || isStopThread || pcmPacket == null || pcmPacket.data == null) {
            droppedPackets++;
            logDrop("Dropping audio packet because player is stopped/disabled or packet is empty");
            return;
        }
        if (!packets.offer(pcmPacket)) {
            droppedPackets++;
            logDrop("Dropping audio packet because queue is full. capacity=" + QUEUE_CAPACITY);
            return;
        }
        queuedPackets++;
        if (shouldLogPacket(queuedPackets)) {
            LogRepository.INSTANCE.addLog(TAG, "Queued PCM packet bytes=" + pcmPacket.data.length +
                    " pts=" + pcmPacket.pts +
                    " queueSize=" + packets.size(), 'I');
        }
    }

    @Override
    public void run() {
        super.run();
        while (!isStopThread && !isInterrupted()) {
            try {
                doPlay(packets.take());
            } catch (InterruptedException e) {
                Log.e(TAG, "run: take error: ", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    private void doPlay(PCMPacket pcmPacket) {
        if (mTrack == null || audioDisabled || pcmPacket == null || pcmPacket.data == null) {
            return;
        }

        if (pcmPacket.data.length == 0) {
            droppedPackets++;
            logDrop("Dropping empty PCM packet");
            return;
        }

        if (pcmPacket.data.length % frameSizeBytes != 0) {
            disableAudio("PCM packet size " + pcmPacket.data.length +
                    " is not aligned to frame size " + frameSizeBytes +
                    " pts=" + pcmPacket.pts);
            return;
        }

        try {
            int written = writeFully(pcmPacket.data);
            if (written < 0) {
                disableAudio("AudioTrack write failed with code " + written +
                        " packetBytes=" + pcmPacket.data.length +
                        " pts=" + pcmPacket.pts);
                return;
            }

            writtenPackets++;
            writtenBytes += written;
            if (written != pcmPacket.data.length) {
                LogRepository.INSTANCE.addLog(TAG, "Partial AudioTrack write. requested=" +
                        pcmPacket.data.length + " written=" + written +
                        " pts=" + pcmPacket.pts, 'W');
            } else if (shouldLogPacket(writtenPackets)) {
                LogRepository.INSTANCE.addLog(TAG, "AudioTrack write ok bytes=" + written +
                        " pts=" + pcmPacket.pts +
                        " totalBytes=" + writtenBytes, 'I');
            }
        } catch (Exception e) {
            Log.e(TAG, "doPlay: error", e);
            disableAudio("AudioTrack exception: " + e.getMessage());
        }
    }

    private int writeFully(byte[] data) {
        int totalWritten = 0;
        while (totalWritten < data.length && !audioDisabled && !isStopThread) {
            int written = mTrack.write(data, totalWritten, data.length - totalWritten,
                    AudioTrack.WRITE_BLOCKING);
            if (written < 0) {
                return written;
            }
            if (written == 0) {
                break;
            }
            totalWritten += written;
        }
        return totalWritten;
    }

    public void stopPlay() {
        isStopThread = true;
        releaseTrack();
        packets.clear();
    }

    public void stopPlayer() {
        isStopThread = true;
        releaseTrack();
        packets.clear();
        interrupt();
        LogRepository.INSTANCE.addLog(TAG, "AudioPlayer thread stopped.", 'I');
    }

    private void disableAudio(String reason) {
        audioDisabled = true;
        packets.clear();
        LogRepository.INSTANCE.addLog(TAG, "Audio disabled: " + reason, 'E');
        releaseTrack();
    }

    private void releaseTrack() {
        AudioTrack track = mTrack;
        mTrack = null;
        if (track == null) {
            return;
        }

        try {
            track.flush();
        } catch (Exception e) {
            Log.e(TAG, "releaseTrack: flush failed", e);
            LogRepository.INSTANCE.addLog(TAG, "AudioTrack flush failed during cleanup: " + e.getMessage(), 'W');
        }
        try {
            if (track.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
                track.stop();
            }
        } catch (Exception e) {
            Log.e(TAG, "releaseTrack: stop failed", e);
            LogRepository.INSTANCE.addLog(TAG, "AudioTrack stop failed during cleanup: " + e.getMessage(), 'W');
        }
        try {
            track.release();
            LogRepository.INSTANCE.addLog(TAG, "AudioTrack released.", 'I');
        } catch (Exception e) {
            Log.e(TAG, "releaseTrack: release failed", e);
            LogRepository.INSTANCE.addLog(TAG, "AudioTrack release failed: " + e.getMessage(), 'E');
        }
    }

    private void logDrop(String reason) {
        if (shouldLogPacket(droppedPackets)) {
            LogRepository.INSTANCE.addLog(TAG, reason + " dropped=" + droppedPackets, 'W');
        }
    }

    private boolean shouldLogPacket(int count) {
        return count <= LOG_FIRST_PACKETS || count % LOG_EVERY_PACKETS == 0;
    }

    private static final class AudioConfig {
        final int sampleRate;
        final int channels;
        final int channelConfig;
        final int audioFormat;
        final int frameSizeBytes;

        private AudioConfig(int sampleRate, int channels, int channelConfig, int audioFormat, int frameSizeBytes) {
            this.sampleRate = sampleRate;
            this.channels = channels;
            this.channelConfig = channelConfig;
            this.audioFormat = audioFormat;
            this.frameSizeBytes = frameSizeBytes;
        }

        static AudioConfig from(AudioStreamInfo audioInfo) {
            if (audioInfo == null || audioInfo.getCompressionType() == null) {
                return null;
            }

            AudioStreamInfo.AudioFormat streamFormat = audioInfo.getAudioFormat();
            if (streamFormat == null) {
                return null;
            }

            AudioStreamInfo.CompressionType compressionType = audioInfo.getCompressionType();
            if (compressionType == AudioStreamInfo.CompressionType.LPCM) {
                if (!streamFormat.isLinearPcm() || streamFormat.getBitDepth() != 16) {
                    return null;
                }
            } else if (compressionType != AudioStreamInfo.CompressionType.AAC &&
                    compressionType != AudioStreamInfo.CompressionType.AAC_ELD) {
                return null;
            }

            int channelConfig;
            if (streamFormat.getChannels() == 1) {
                channelConfig = AudioFormat.CHANNEL_OUT_MONO;
            } else if (streamFormat.getChannels() == 2) {
                channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
            } else {
                return null;
            }

            int frameSizeBytes = streamFormat.getChannels() * 2;
            return new AudioConfig(streamFormat.getSampleRate(), streamFormat.getChannels(),
                    channelConfig, AudioFormat.ENCODING_PCM_16BIT, frameSizeBytes);
        }
    }
}
