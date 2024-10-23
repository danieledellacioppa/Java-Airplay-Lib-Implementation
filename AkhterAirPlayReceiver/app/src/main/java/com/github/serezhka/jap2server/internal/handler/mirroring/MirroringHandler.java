package com.github.serezhka.jap2server.internal.handler.mirroring;

import android.util.Log;

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

    private final ByteBuf headerBuf = ByteBufAllocator.DEFAULT.ioBuffer(128, 128);
    private final AirPlay airPlay;
    private final AirplayDataConsumer dataConsumer;

    private MirroringHeader header;
    private ByteBuf payload;

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
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        headerBuf.release();  // Rilascia il buffer quando il gestore viene rimosso
        super.handlerRemoved(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        while (msg.isReadable()) {

            if (header == null) {
                msg.readBytes(headerBuf, Math.min(headerBuf.writableBytes(), msg.readableBytes()));
                if (headerBuf.writableBytes() == 0) {
                    header = new MirroringHeader(headerBuf);
                    headerBuf.clear();
                }
            }

            if (header != null && msg.readableBytes() > 0) {

                if (payload == null || payload.writableBytes() == 0) {
                    payload = ctx.alloc().directBuffer(header.getPayloadSize(), header.getPayloadSize());
                }

                msg.readBytes(payload, Math.min(payload.writableBytes(), msg.readableBytes()));

                if (payload.writableBytes() == 0) {
                    byte[] payloadBytes = new byte[header.getPayloadSize()];
                    try {
                        payload.readBytes(payloadBytes);

                        if (header.getPayloadType() == 0) {
                            airPlay.decryptVideo(payloadBytes);
                            processVideo(payloadBytes);
                        } else if (header.getPayloadType() == 1) {
                            processSPSPPS(payload);
                        } else {
                            log.debug("Unhandled payload type: {}", header.getPayloadType());
                        }
                    } catch (Exception e) {
                        log.error("Error processing payload", e);
                    } finally {
                        payload.release();  // Assicurati che venga rilasciato sempre
                        payload = null;
                        header = null;
                    }
                }
            }
        }
    }

    private void processVideo(byte[] payload) {

        // TODO One nalu per packet?
        int nalu_size = 0;
        while (nalu_size < payload.length) {
            int nc_len = (payload[nalu_size + 3] & 0xFF) | ((payload[nalu_size + 2] & 0xFF) << 8) | ((payload[nalu_size + 1] & 0xFF) << 16) | ((payload[nalu_size] & 0xFF) << 24);
            log.debug("payload len: {}, nc_len: {}, nalu_type: {}", payload.length, nc_len, payload[4] & 0x1f);
            if (nc_len > 0) {
                payload[nalu_size] = 0;
                payload[nalu_size + 1] = 0;
                payload[nalu_size + 2] = 0;
                payload[nalu_size + 3] = 1;
                nalu_size += nc_len + 4;
            }
            if (payload.length - nc_len > 4) {
                log.error("Decrypt error!");
                Log.e("MirroringHandler", "Decrypt error!");
                return;
            }
        }

        dataConsumer.onVideo(payload);
    }

    private void processSPSPPS(ByteBuf payload) {
        payload.readerIndex(6);

        short spsLen = (short) payload.readUnsignedShort();
        byte[] sequenceParameterSet = new byte[spsLen];
        payload.readBytes(sequenceParameterSet);

        payload.skipBytes(1); // pps count

        short ppsLen = (short) payload.readUnsignedShort();
        byte[] pictureParameterSet = new byte[ppsLen];
        payload.readBytes(pictureParameterSet);

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

        dataConsumer.onVideo(spsPps);
    }
}
