package com.github.serezhka.jap2server.internal.handler.audio;

import com.cjx.airplayjavademo.tools.LogRepository;
import com.github.serezhka.jap2lib.AirPlay;
import com.github.serezhka.jap2lib.rtsp.AudioStreamInfo;
import com.github.serezhka.jap2server.AirplayDataConsumer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class AudioHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private static final Logger log = LoggerFactory.getLogger(AudioHandler.class);
    private static final String TAG = "AudioHandler";
    private static final int RTP_HEADER_SIZE = 12;
    private static final int LOG_FIRST_PACKETS = 5;
    private static final int LOG_EVERY_PACKETS = 100;
    private static final boolean AIRPLAY_AUDIO_FORWARDING_ENABLED = false;

    private final AirPlay airPlay;
    private final AirplayDataConsumer dataConsumer;
    private final AudioStreamInfo audioStreamInfo;

    private final AudioPacket[] buffer = new AudioPacket[512];

    private int prevSeqNum;
    private int packetsInBuffer;
    private int packetsReceived;
    private int packetsForwarded;
    private int packetsDropped;
    private volatile boolean audioForwardingEnabled;

    public AudioHandler(AirPlay airPlay, AirplayDataConsumer dataConsumer, AudioStreamInfo audioStreamInfo) {
        this.airPlay = airPlay;
        this.dataConsumer = dataConsumer;
        this.audioStreamInfo = audioStreamInfo;
        this.audioForwardingEnabled = AIRPLAY_AUDIO_FORWARDING_ENABLED && isLpcm16(audioStreamInfo);
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = new AudioPacket();
        }
        if (audioForwardingEnabled) {
            LogRepository.INSTANCE.addLog(TAG, "Audio RTP forwarding enabled for " + describeAudioInfo(audioStreamInfo), 'I');
        } else {
            LogRepository.INSTANCE.addLog(TAG, "Audio RTP forwarding disabled. AirPlay audio is muted during video crash investigation. " +
                    describeAudioInfo(audioStreamInfo), 'W');
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) {
        try {
            readAudioPacket(msg);
        } catch (Exception e) {
            packetsDropped++;
            log.error("Dropping audio packet after handler error", e);
            LogRepository.INSTANCE.addLog(TAG, "Dropping audio packet after handler error: " + e.getMessage(), 'E');
        }
    }

    private void readAudioPacket(DatagramPacket msg) throws Exception {
        ByteBuf content = msg.content();

        if (content.readableBytes() < RTP_HEADER_SIZE) {
            packetsDropped++;
            LogRepository.INSTANCE.addLog(TAG, "Dropping short audio packet. bytes=" + content.readableBytes(), 'W');
            return;
        }

        byte[] headerBytes = new byte[RTP_HEADER_SIZE];
        content.readBytes(headerBytes);

        int flag = headerBytes[0] & 0xFF;
        int type = headerBytes[1] & 0x7F;

        int curSeqNo = ((headerBytes[2] & 0xFF) << 8) | (headerBytes[3] & 0xFF);

        long timestamp = ((headerBytes[4] & 0xFFL) << 24) |
                ((headerBytes[5] & 0xFFL) << 16) |
                ((headerBytes[6] & 0xFFL) << 8) |
                (headerBytes[7] & 0xFFL);

        long ssrc = ((headerBytes[8] & 0xFFL) << 24) |
                ((headerBytes[9] & 0xFFL) << 16) |
                ((headerBytes[10] & 0xFFL) << 8) |
                (headerBytes[11] & 0xFFL);

        int payloadSize = content.readableBytes();
        packetsReceived++;

        if (shouldLogPacket(packetsReceived)) {
            LogRepository.INSTANCE.addLog(TAG, "Audio RTP header seq=" + curSeqNo +
                    " ts=" + timestamp +
                    " ssrc=" + ssrc +
                    " flag=" + flag +
                    " type=" + type +
                    " payload=" + payloadSize +
                    " forwarding=" + audioForwardingEnabled, 'I');
        }

        if (!audioForwardingEnabled) {
            packetsDropped++;
            if (shouldLogPacket(packetsDropped)) {
                LogRepository.INSTANCE.addLog(TAG, "Dropping unsupported audio payload before decrypt/decode. seq=" +
                        curSeqNo + " payload=" + payloadSize + " format=" + describeAudioInfo(audioStreamInfo), 'W');
            }
            return;
        }

        // TODO handle bad cases (missing packets, curSeqNum - prevSeqNum > buffer.length, ...)
        if (curSeqNo <= prevSeqNum) {
            packetsDropped++;
            if (shouldLogPacket(packetsDropped)) {
                LogRepository.INSTANCE.addLog(TAG, "Dropping old/duplicate audio packet seq=" + curSeqNo +
                        " prevSeq=" + prevSeqNum, 'W');
            }
            return;
        }

        log.debug("Got audio packet. flag: {}, type: {}, prevSeqNum: {}, curSecNum: {}, audio packets in buffer: {}",
                flag, type, prevSeqNum, curSeqNo, packetsInBuffer);

        AudioPacket audioPacket = buffer[curSeqNo % buffer.length];
        if (payloadSize > audioPacket.getEncodedAudioCapacity()) {
            packetsDropped++;
            audioForwardingEnabled = false;
            LogRepository.INSTANCE.addLog(TAG, "Disabling audio: payload " + payloadSize +
                    " exceeds buffer capacity " + audioPacket.getEncodedAudioCapacity() +
                    " for seq=" + curSeqNo, 'E');
            return;
        }

        audioPacket
                .flag(flag)
                .type(type)
                .sequenceNumber(curSeqNo)
                .timestamp(timestamp)
                .ssrc(ssrc)
                .available(true)
                .encodedAudioSize(payloadSize)
                .encodedAudio(packet -> content.readBytes(packet, 0, payloadSize));
        packetsInBuffer++;

        while (dequeue(curSeqNo)) {
            curSeqNo++;
        }
    }

    private boolean dequeue(int curSeqNo) {
        if (!audioForwardingEnabled) {
            return false;
        }
        if (curSeqNo - prevSeqNum == 1 || prevSeqNum == 0) {
            AudioPacket audioPacket = buffer[curSeqNo % buffer.length];
            if (audioPacket.isAvailable()) {
                try {
                    if (shouldLogPacket(packetsForwarded + 1)) {
                        LogRepository.INSTANCE.addLog(TAG, "Decrypting audio seq=" +
                                audioPacket.getSequenceNumber() +
                                " ts=" + audioPacket.getTimestamp() +
                                " bytes=" + audioPacket.getEncodedAudioSize(), 'I');
                    }
                    airPlay.decryptAudio(audioPacket.getEncodedAudio(), audioPacket.getEncodedAudioSize());
                    byte[] decodedPcm = Arrays.copyOfRange(audioPacket.getEncodedAudio(), 0, audioPacket.getEncodedAudioSize());
                    packetsForwarded++;
                    if (shouldLogPacket(packetsForwarded)) {
                        LogRepository.INSTANCE.addLog(TAG, "Forwarding decrypted LPCM seq=" +
                                audioPacket.getSequenceNumber() +
                                " ts=" + audioPacket.getTimestamp() +
                                " bytes=" + decodedPcm.length +
                                " firstBytes=" + firstBytesHex(decodedPcm, 8), 'I');
                    }
                    dataConsumer.onAudio(decodedPcm, audioPacket.getTimestamp(), audioPacket.getSequenceNumber());
                } catch (Exception e) {
                    packetsDropped++;
                    audioForwardingEnabled = false;
                    log.error("Disabling audio after decrypt/forward failure", e);
                    LogRepository.INSTANCE.addLog(TAG, "Disabling audio after decrypt/forward failure: " +
                            e.getMessage(), 'E');
                }
                audioPacket.available(false);
                prevSeqNum = curSeqNo;
                packetsInBuffer--;
                return true;
            }
        }
        return false;
    }

    private boolean isLpcm16(AudioStreamInfo info) {
        if (info == null || info.getCompressionType() != AudioStreamInfo.CompressionType.LPCM) {
            return false;
        }
        AudioStreamInfo.AudioFormat format = info.getAudioFormat();
        return format != null && format.isLinearPcm() && format.getBitDepth() == 16 &&
                (format.getChannels() == 1 || format.getChannels() == 2);
    }

    private boolean shouldLogPacket(int count) {
        return count <= LOG_FIRST_PACKETS || count % LOG_EVERY_PACKETS == 0;
    }

    private String describeAudioInfo(AudioStreamInfo info) {
        if (info == null) {
            return "unknown audio format";
        }
        AudioStreamInfo.AudioFormat format = info.getAudioFormat();
        String details = format == null ? "format=unknown" :
                "format=" + format +
                        " sampleRate=" + format.getSampleRate() +
                        " channels=" + format.getChannels() +
                        " bitDepth=" + format.getBitDepth();
        return "compression=" + info.getCompressionType() +
                " compressionCode=" + info.getCompressionTypeCode() +
                " " + details +
                " formatCode=" + info.getAudioFormatCode() +
                " spf=" + info.getSamplesPerFrame();
    }

    private String firstBytesHex(byte[] bytes, int maxBytes) {
        StringBuilder builder = new StringBuilder();
        int length = Math.min(bytes.length, maxBytes);
        for (int i = 0; i < length; i++) {
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
}
