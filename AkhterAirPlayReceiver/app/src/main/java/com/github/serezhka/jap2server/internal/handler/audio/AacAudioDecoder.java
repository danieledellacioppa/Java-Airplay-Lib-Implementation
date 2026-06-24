package com.github.serezhka.jap2server.internal.handler.audio;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import com.cjx.airplayjavademo.tools.LogRepository;
import com.github.serezhka.jap2lib.rtsp.AudioStreamInfo;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

final class AacAudioDecoder implements AutoCloseable {

    private static final String TAG = "AacAudioDecoder";
    private static final String MIME_AAC = "audio/mp4a-latm";
    private static final String KEY_AAC_PROFILE = "aac-profile";
    private static final int AAC_OBJECT_LC = 2;
    private static final int AAC_OBJECT_ELD = 39;
    private static final long CODEC_TIMEOUT_US = 1000;

    private final MediaCodec decoder;
    private final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
    private final int sampleRate;
    private int decodedPackets;
    private boolean closed;

    static boolean isSupported(AudioStreamInfo audioInfo) {
        if (audioInfo == null || audioInfo.getAudioFormat() == null) {
            return false;
        }
        AudioStreamInfo.CompressionType compressionType = audioInfo.getCompressionType();
        if (compressionType != AudioStreamInfo.CompressionType.AAC &&
                compressionType != AudioStreamInfo.CompressionType.AAC_ELD) {
            return false;
        }
        int channels = audioInfo.getAudioFormat().getChannels();
        return channels == 1 || channels == 2;
    }

    AacAudioDecoder(AudioStreamInfo audioInfo) throws Exception {
        if (!isSupported(audioInfo)) {
            throw new IllegalArgumentException("Unsupported AAC stream: " + audioInfo);
        }

        AudioStreamInfo.AudioFormat streamFormat = audioInfo.getAudioFormat();
        sampleRate = streamFormat.getSampleRate();
        int channels = streamFormat.getChannels();
        int audioObjectType = audioObjectType(audioInfo.getCompressionType());
        byte[] csd = buildAudioSpecificConfig(audioObjectType, sampleRate, channels);

        MediaFormat mediaFormat = MediaFormat.createAudioFormat(MIME_AAC, sampleRate, channels);
        mediaFormat.setInteger(MediaFormat.KEY_IS_ADTS, 0);
        mediaFormat.setInteger(KEY_AAC_PROFILE, audioObjectType);
        mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(csd));

        decoder = MediaCodec.createDecoderByType(MIME_AAC);
        decoder.configure(mediaFormat, null, null, 0);
        decoder.start();

        LogRepository.INSTANCE.addLog(TAG, "MediaCodec AAC decoder started. objectType=" +
                audioObjectType + " sampleRate=" + sampleRate + " channels=" + channels +
                " csd=" + bytesToHex(csd), 'I');
    }

    byte[] decode(byte[] input, int inputSize, long rtpTimestamp) throws Exception {
        if (closed || inputSize <= 0) {
            return new byte[0];
        }

        int inputBufferIndex = decoder.dequeueInputBuffer(CODEC_TIMEOUT_US);
        if (inputBufferIndex < 0) {
            LogRepository.INSTANCE.addLog(TAG, "No AAC decoder input buffer available; dropping frame", 'W');
            return new byte[0];
        }

        ByteBuffer inputBuffer = decoder.getInputBuffer(inputBufferIndex);
        if (inputBuffer == null) {
            LogRepository.INSTANCE.addLog(TAG, "AAC decoder returned null input buffer", 'W');
            return new byte[0];
        }

        inputBuffer.clear();
        inputBuffer.put(input, 0, inputSize);
        decoder.queueInputBuffer(inputBufferIndex, 0, inputSize, rtpTimestampToUs(rtpTimestamp), 0);

        ByteArrayOutputStream pcm = new ByteArrayOutputStream();
        while (true) {
            int outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, CODEC_TIMEOUT_US);
            if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                break;
            }
            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                LogRepository.INSTANCE.addLog(TAG, "AAC decoder output format changed: " +
                        decoder.getOutputFormat(), 'I');
                continue;
            }
            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                continue;
            }
            if (outputBufferIndex < 0) {
                continue;
            }

            ByteBuffer outputBuffer = decoder.getOutputBuffer(outputBufferIndex);
            if (outputBuffer != null && bufferInfo.size > 0) {
                outputBuffer.position(bufferInfo.offset);
                outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                byte[] chunk = new byte[bufferInfo.size];
                outputBuffer.get(chunk);
                pcm.write(chunk, 0, chunk.length);
            }
            decoder.releaseOutputBuffer(outputBufferIndex, false);
        }

        byte[] decoded = pcm.toByteArray();
        decodedPackets++;
        if (decoded.length > 0 && (decodedPackets <= 5 || decodedPackets % 100 == 0)) {
            LogRepository.INSTANCE.addLog(TAG, "Decoded AAC packet bytesIn=" + inputSize +
                    " bytesPcm=" + decoded.length +
                    " ptsUs=" + rtpTimestampToUs(rtpTimestamp), 'I');
        }
        return decoded;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            decoder.stop();
        } catch (Exception e) {
            Log.w(TAG, "Failed to stop AAC decoder", e);
            LogRepository.INSTANCE.addLog(TAG, "Failed to stop AAC decoder: " + e.getMessage(), 'W');
        }
        try {
            decoder.release();
            LogRepository.INSTANCE.addLog(TAG, "AAC decoder released.", 'I');
        } catch (Exception e) {
            Log.e(TAG, "Failed to release AAC decoder", e);
            LogRepository.INSTANCE.addLog(TAG, "Failed to release AAC decoder: " + e.getMessage(), 'E');
        }
    }

    private long rtpTimestampToUs(long rtpTimestamp) {
        return rtpTimestamp * 1_000_000L / sampleRate;
    }

    private static int audioObjectType(AudioStreamInfo.CompressionType compressionType) {
        return compressionType == AudioStreamInfo.CompressionType.AAC_ELD ? AAC_OBJECT_ELD : AAC_OBJECT_LC;
    }

    private static byte[] buildAudioSpecificConfig(int audioObjectType, int sampleRate, int channels) {
        BitWriter writer = new BitWriter();
        writeAudioObjectType(writer, audioObjectType);
        int frequencyIndex = samplingFrequencyIndex(sampleRate);
        if (frequencyIndex >= 0) {
            writer.write(frequencyIndex, 4);
        } else {
            writer.write(0x0F, 4);
            writer.write(sampleRate, 24);
        }
        writer.write(channels, 4);

        if (audioObjectType == AAC_OBJECT_ELD) {
            writer.write(0, 1); // frameLengthFlag
            writer.write(0, 1); // aacSectionDataResilienceFlag
            writer.write(0, 1); // aacScalefactorDataResilienceFlag
            writer.write(0, 1); // aacSpectralDataResilienceFlag
            writer.write(0, 1); // ldSbrPresentFlag
            writer.write(0, 4); // eldExtType: terminator
            writer.write(0, 2); // epConfig
        }

        return writer.toByteArray();
    }

    private static void writeAudioObjectType(BitWriter writer, int audioObjectType) {
        if (audioObjectType <= 31) {
            writer.write(audioObjectType, 5);
            return;
        }
        writer.write(31, 5);
        writer.write(audioObjectType - 32, 6);
    }

    private static int samplingFrequencyIndex(int sampleRate) {
        switch (sampleRate) {
            case 96000:
                return 0;
            case 88200:
                return 1;
            case 64000:
                return 2;
            case 48000:
                return 3;
            case 44100:
                return 4;
            case 32000:
                return 5;
            case 24000:
                return 6;
            case 22050:
                return 7;
            case 16000:
                return 8;
            case 12000:
                return 9;
            case 11025:
                return 10;
            case 8000:
                return 11;
            case 7350:
                return 12;
            default:
                return -1;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) {
                builder.append(' ');
            }
            String hex = Integer.toHexString(bytes[i] & 0xFF).toUpperCase();
            if (hex.length() == 1) {
                builder.append('0');
            }
            builder.append(hex);
        }
        return builder.toString();
    }

    private static final class BitWriter {
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private int currentByte;
        private int bitCount;

        void write(int value, int bits) {
            for (int i = bits - 1; i >= 0; i--) {
                currentByte = (currentByte << 1) | ((value >> i) & 1);
                bitCount++;
                if (bitCount == 8) {
                    bytes.write(currentByte);
                    currentByte = 0;
                    bitCount = 0;
                }
            }
        }

        byte[] toByteArray() {
            if (bitCount > 0) {
                bytes.write(currentByte << (8 - bitCount));
                currentByte = 0;
                bitCount = 0;
            }
            return bytes.toByteArray();
        }
    }
}
