package com.github.serezhka.jap2server.internal.handler.mirroring;

import com.cjx.airplayjavademo.tools.LogRepository;
import com.github.serezhka.jap2lib.AirPlay;
import com.github.serezhka.jap2server.AirplayDataConsumer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <h1 style="color: #2e6c80;">MirroringHandler</h1>
 *
 * <p style="font-size: 1.1em; color: #555555;">
 * The <strong>MirroringHandler</strong> class is responsible for managing and handling incoming mirroring data over the AirPlay protocol.
 * This class processes <code>ByteBuf</code> messages, decrypts video streams, and forwards the data to the
 * <strong>AirplayDataConsumer</strong> for playback.
 * </p>
 *
 * <h2 style="color: #3a79a1;">Main Features</h2>
 * <ul>
 *   <li>Processes AirPlay mirroring data, interpreting payload headers and content.</li>
 *   <li>Decrypts video data and forwards it to the data consumer.</li>
 *   <li>Handles SPS/PPS data for video stream configuration.</li>
 * </ul>
 *
 * <h2 style="color: #3a79a1;">Methods</h2>
 * <ul>
 *   <li><strong>channelRead0(ChannelHandlerContext ctx, ByteBuf msg)</strong>: Handles incoming <code>ByteBuf</code> messages, processes headers, and manages video/audio payloads.</li>
 *   <li><strong>processVideo(byte[] payload)</strong>: Processes and structures NAL units from the video payload and forwards them to the consumer.</li>
 *   <li><strong>processSPSPPS(ByteBuf payload)</strong>: Processes SPS and PPS data from the payload and prepares it for video configuration.</li>
 * </ul>
 *
 * <h2 style="color: #3a79a1;">Payload Types</h2>
 * <ul>
 *   <li><strong>0</strong>: Video data that needs decryption and processing.</li>
 *   <li><strong>1</strong>: SPS/PPS data required for configuring the video stream.</li>
 * </ul>
 *
 * <h2 style="color: #3a79a1;">Usage</h2>
 * <p style="font-size: 1.1em; color: #555555;">
 * This class is used in a Netty pipeline for handling real-time AirPlay mirroring streams. It ensures proper decoding and forwarding
 * of video and SPS/PPS data to a media player or data consumer.
 * </p>
 *
 * <h2 style="color: #3a79a1;">Parameters</h2>
 * <ul>
 *   <li><strong>airPlay</strong>: The <strong>AirPlay</strong> instance used for decrypting video data.</li>
 *   <li><strong>dataConsumer</strong>: The <strong>AirplayDataConsumer</strong> instance responsible for processing the video data.</li>
 * </ul>
 */
public class MirroringHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Logger log = LoggerFactory.getLogger(MirroringHandler.class);
    private static final String TAG = "MirroringHandler";
    private static final int HEADER_SIZE = 128;
    private static final int MAX_PAYLOAD_SIZE = 8 * 1024 * 1024;
    private static final int LOG_FIRST_PACKETS = 5;
    private static final int LOG_EVERY_PACKETS = 100;

    private final ByteBuf headerBuf = ByteBufAllocator.DEFAULT.ioBuffer(128, 128);
    private final AirPlay airPlay;
    private final AirplayDataConsumer dataConsumer;

    private MirroringHeader header;
    private ByteBuf payload;
    private long payloadSequence;
    private long videoSequence;
    private long droppedPayloads;

    /**
     * Creates a new MirroringHandler instance with the specified AirPlay instance and data consumer.
     *
     * @param airPlay The AirPlay instance used for decrypting video data.
     * @param dataConsumer The AirplayDataConsumer instance that processes the video data.
     */
    public MirroringHandler(AirPlay airPlay, AirplayDataConsumer dataConsumer) {
        this.airPlay = airPlay;
        this.dataConsumer = dataConsumer;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        try {
            while (msg.isReadable()) {

                if (header == null) {
                    msg.readBytes(headerBuf, Math.min(headerBuf.writableBytes(), msg.readableBytes()));
                    if (headerBuf.writableBytes() == 0) {
                        header = new MirroringHeader(headerBuf);
                        headerBuf.clear();
                        payloadSequence++;
                        logHeader();
                        if (!isPayloadSizeValid(header.getPayloadSize())) {
                            droppedPayloads++;
                            closeAfterError(ctx, "Invalid mirroring payload size " + header.getPayloadSize(), null);
                            resetPacketState();
                            return;
                        }
                    }
                }

                if (header != null && msg.readableBytes() > 0) {

                    if (payload == null) {
                        payload = ctx.alloc().directBuffer(header.getPayloadSize(), header.getPayloadSize());
                    }

                    msg.readBytes(payload, Math.min(payload.writableBytes(), msg.readableBytes()));

                    if (payload.writableBytes() == 0) {

                        byte[] payloadBytes = new byte[header.getPayloadSize()];
                        payload.readBytes(payloadBytes);

                        try {
                            long timestampUs = System.nanoTime() / 1000L;
                            if (header.getPayloadType() == 0) {
                                airPlay.decryptVideo(payloadBytes);
                                processVideo(payloadBytes, timestampUs);
                            } else if (header.getPayloadType() == 1) {
                                processSPSPPS(payloadBytes, timestampUs);
                            } else {
                                log.debug("Unhandled payload type: {}", header.getPayloadType());
                                if (shouldLogPacket(payloadSequence)) {
                                    LogRepository.INSTANCE.addLog(TAG, "Unhandled mirroring payload type=" +
                                            header.getPayloadType() + " size=" + header.getPayloadSize(), 'W');
                                }
                            }
                        } catch (Exception e) {
                            droppedPayloads++;
                            closeAfterError(ctx, "Mirroring payload processing failed at seq=" + payloadSequence +
                                    " type=" + header.getPayloadType(), e);
                            return;
                        } finally {
                            resetPacketState();
                        }
                    }
                }
            }
        } catch (Exception e) {
            droppedPayloads++;
            closeAfterError(ctx, "Mirroring read loop failed at seq=" + payloadSequence, e);
            resetPacketState();
        }
    }

    private void processVideo(byte[] payload, long timestampUs) {
        long currentVideoSequence = ++videoSequence;
        if (payload.length < 4) {
            droppedPayloads++;
            LogRepository.INSTANCE.addLog(TAG, "Dropping short video payload seq=" + currentVideoSequence +
                    " bytes=" + payload.length, 'W');
            return;
        }

        int offset = 0;
        int nalUnits = 0;
        int firstNalType = -1;
        while (offset < payload.length) {
            if (payload.length - offset < 4) {
                droppedPayloads++;
                LogRepository.INSTANCE.addLog(TAG, "Dropping malformed video payload seq=" + currentVideoSequence +
                        " trailingBytes=" + (payload.length - offset), 'E');
                return;
            }

            int nalLength = readIntBE(payload, offset);
            int nalStart = offset + 4;
            int nextOffset = nalStart + nalLength;
            if (nalLength <= 0 || nextOffset > payload.length) {
                droppedPayloads++;
                LogRepository.INSTANCE.addLog(TAG, "Dropping malformed video payload seq=" + currentVideoSequence +
                        " offset=" + offset +
                        " nalLength=" + nalLength +
                        " payloadBytes=" + payload.length, 'E');
                return;
            }

            int nalType = payload[nalStart] & 0x1F;
            if (firstNalType < 0) {
                firstNalType = nalType;
            }

            payload[offset] = 0;
            payload[offset + 1] = 0;
            payload[offset + 2] = 0;
            payload[offset + 3] = 1;
            nalUnits++;
            offset = nextOffset;
        }

        if (shouldLogPacket(currentVideoSequence)) {
            LogRepository.INSTANCE.addLog(TAG, "Forwarding video payload seq=" + currentVideoSequence +
                    " ptsUs=" + timestampUs +
                    " bytes=" + payload.length +
                    " nalUnits=" + nalUnits +
                    " firstNalType=" + firstNalType, 'I');
        }

        dataConsumer.onVideo(payload, timestampUs, currentVideoSequence, false);
    }

    private void processSPSPPS(byte[] payload, long timestampUs) {
        long currentVideoSequence = ++videoSequence;
        if (payload.length < 9) {
            droppedPayloads++;
            LogRepository.INSTANCE.addLog(TAG, "Dropping short SPS/PPS payload seq=" + currentVideoSequence +
                    " bytes=" + payload.length, 'E');
            return;
        }

        int offset = 6;
        int spsLen = readUnsignedShortBE(payload, offset);
        offset += 2;
        if (spsLen <= 0 || offset + spsLen + 3 > payload.length) {
            droppedPayloads++;
            LogRepository.INSTANCE.addLog(TAG, "Dropping malformed SPS payload seq=" + currentVideoSequence +
                    " spsLen=" + spsLen + " payloadBytes=" + payload.length, 'E');
            return;
        }

        byte[] sequenceParameterSet = new byte[spsLen];
        System.arraycopy(payload, offset, sequenceParameterSet, 0, spsLen);
        offset += spsLen;

        offset += 1; // pps count
        if (offset + 2 > payload.length) {
            droppedPayloads++;
            LogRepository.INSTANCE.addLog(TAG, "Dropping malformed SPS/PPS payload seq=" + currentVideoSequence +
                    " missing PPS length", 'E');
            return;
        }

        int ppsLen = readUnsignedShortBE(payload, offset);
        offset += 2;
        if (ppsLen <= 0 || offset + ppsLen > payload.length) {
            droppedPayloads++;
            LogRepository.INSTANCE.addLog(TAG, "Dropping malformed PPS payload seq=" + currentVideoSequence +
                    " ppsLen=" + ppsLen + " payloadBytes=" + payload.length, 'E');
            return;
        }
        byte[] pictureParameterSet = new byte[ppsLen];
        System.arraycopy(payload, offset, pictureParameterSet, 0, ppsLen);

        int spsPpsLen = spsLen + ppsLen + 8;
        log.info("SPS PPS length: {}", spsPpsLen);
        byte[] spsPps = new byte[spsPpsLen];
        spsPps[0] = 0;
        spsPps[1] = 0;
        spsPps[2] = 0;
        spsPps[3] = 1;
        System.arraycopy(sequenceParameterSet, 0, spsPps, 4, spsLen);
        spsPps[spsLen + 4] = 0;
        spsPps[spsLen + 5] = 0;
        spsPps[spsLen + 6] = 0;
        spsPps[spsLen + 7] = 1;
        System.arraycopy(pictureParameterSet, 0, spsPps, 8 + spsLen, ppsLen);

        LogRepository.INSTANCE.addLog(TAG, "Forwarding SPS/PPS seq=" + currentVideoSequence +
                " ptsUs=" + timestampUs +
                " spsLen=" + spsLen +
                " ppsLen=" + ppsLen, 'I');

        dataConsumer.onVideo(spsPps, timestampUs, currentVideoSequence, true);
    }

    private boolean isPayloadSizeValid(int payloadSize) {
        return payloadSize > 0 && payloadSize <= MAX_PAYLOAD_SIZE;
    }

    private void logHeader() {
        if (shouldLogPacket(payloadSequence)) {
            LogRepository.INSTANCE.addLog(TAG, "Mirroring header seq=" + payloadSequence +
                    " type=" + header.getPayloadType() +
                    " option=" + header.getPayloadOption() +
                    " payloadSize=" + header.getPayloadSize() +
                    " source=" + header.getWidthSource() + "x" + header.getHeightSource() +
                    " display=" + header.getWidth() + "x" + header.getHeight(), 'I');
        }
    }

    private boolean shouldLogPacket(long count) {
        return count <= LOG_FIRST_PACKETS || count % LOG_EVERY_PACKETS == 0;
    }

    private int readIntBE(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24) |
                ((bytes[offset + 1] & 0xFF) << 16) |
                ((bytes[offset + 2] & 0xFF) << 8) |
                (bytes[offset + 3] & 0xFF);
    }

    private int readUnsignedShortBE(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
    }

    private void closeAfterError(ChannelHandlerContext ctx, String message, Exception e) {
        if (e != null) {
            log.error(message, e);
            LogRepository.INSTANCE.addLog(TAG, message + ": " + e.getMessage(), 'E');
        } else {
            log.error(message);
            LogRepository.INSTANCE.addLog(TAG, message, 'E');
        }
        if (ctx.channel().isOpen()) {
            ctx.close();
        }
    }

    private void resetPacketState() {
        if (payload != null) {
            payload.release();
            payload = null;
        }
        header = null;
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        LogRepository.INSTANCE.addLog(TAG, "Handler removed. payloadSeq=" + payloadSequence +
                " videoSeq=" + videoSequence +
                " dropped=" + droppedPayloads, 'I');
        if (headerBuf.refCnt() > 0) {
            headerBuf.release();
        }
        if (payload != null) {
            payload.release();
            payload = null;
        }
        super.handlerRemoved(ctx);
    }
}
