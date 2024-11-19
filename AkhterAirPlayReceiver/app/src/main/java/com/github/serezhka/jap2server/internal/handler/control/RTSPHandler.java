package com.github.serezhka.jap2server.internal.handler.control;

import android.util.Log;

import com.cjx.airplayjavademo.tools.LogRepository;
import com.github.serezhka.jap2lib.rtsp.AudioStreamInfo;
import com.github.serezhka.jap2lib.rtsp.MediaStreamInfo;
import com.github.serezhka.jap2lib.rtsp.VideoStreamInfo;
import com.github.serezhka.jap2server.AirplayDataConsumer;
import com.github.serezhka.jap2server.internal.AudioControlServer;
import com.github.serezhka.jap2server.internal.AudioReceiver;
import com.github.serezhka.jap2server.internal.MirroringReceiver;
import com.github.serezhka.jap2server.internal.handler.audio.AudioHandler;
import com.github.serezhka.jap2server.internal.handler.mirroring.MirroringHandler;
import com.github.serezhka.jap2server.internal.handler.session.Session;
import com.github.serezhka.jap2server.internal.handler.session.SessionManager;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.rtsp.RtspMethods;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

@ChannelHandler.Sharable
public class RTSPHandler extends ControlHandler {

    private static final Logger log = LoggerFactory.getLogger(RTSPHandler.class);

    private final AirplayDataConsumer airplayDataConsumer;
    private final int airPlayPort;
    private final int airTunesPort;
    private static final String TAG = "RTSPHandler";

    public RTSPHandler(int airPlayPort, int airTunesPort, SessionManager sessionManager,
                       AirplayDataConsumer airplayDataConsumer) {
        super(sessionManager);
        this.airplayDataConsumer = airplayDataConsumer;
        this.airPlayPort = airPlayPort;
        this.airTunesPort = airTunesPort;
    }

    @Override
    protected boolean handleRequest(ChannelHandlerContext ctx, Session session, FullHttpRequest request) throws Exception {
        DefaultFullHttpResponse response = createResponseForRequest(request);
        if (RtspMethods.SETUP.equals(request.method())) {
            LogRepository.INSTANCE.addLog(TAG, "RTSP SETUP request received", 'I');

            // Controllo se una sessione di mirroring è già attiva
            if (session.isMirroringActive()) {
                Log.d(TAG, "Session already active. Terminating previous session...");
                session.stopMirroring();  // Chiude eventuali sessioni aperte

                // Attendi che il thread precedente termini
                Thread previousThread = session.getAirPlayReceiverThread();
                if (previousThread != null && previousThread.isAlive()) {
                    try {
                        Log.d(TAG, "Waiting for previous MirroringReceiver thread to terminate...");
                        LogRepository.INSTANCE.addLog(TAG, "Waiting for previous MirroringReceiver thread to terminate...", 'W');
                        previousThread.join();
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Interrupted while waiting for previous MirroringReceiver thread to terminate", e);
                        LogRepository.INSTANCE.addLog(TAG, "Interrupted while waiting for previous MirroringReceiver thread to terminate", 'E');
                        Thread.currentThread().interrupt();
                    }
                }
            }

            MediaStreamInfo mediaStreamInfo = session.getAirPlay().rtspGetMediaStreamInfo(new ByteBufInputStream(request.content()), request.method().toString());
            LogRepository.INSTANCE.addLog(TAG, "Media stream info: " + mediaStreamInfo, 'I');

            if (mediaStreamInfo == null) {
                request.content().resetReaderIndex();
                LogRepository.INSTANCE.addLog(TAG, "Media stream info is null", 'E');
                session.getAirPlay().rtspSetupEncryption(new ByteBufInputStream(request.content()));
                LogRepository.INSTANCE.addLog(TAG, "RTSP SETUP ENCRYPTION request received", 'I');
            } else {
                switch (mediaStreamInfo.getStreamType()) {
                    case AUDIO:
                        AudioStreamInfo audioStreamInfo = (AudioStreamInfo) mediaStreamInfo;

                        log.info("Audio format is: {}", audioStreamInfo.getAudioFormat());
                        log.info("Audio compression type is: {}", audioStreamInfo.getCompressionType());
                        log.info("Audio samples per frame is: {}", audioStreamInfo.getSamplesPerFrame());

                        airplayDataConsumer.onAudioFormat(audioStreamInfo);

                        AudioHandler audioHandler = new AudioHandler(session.getAirPlay(), airplayDataConsumer);
                        AudioReceiver audioReceiver = new AudioReceiver(audioHandler, this);
                        Thread audioReceiverThread = new Thread(audioReceiver);
                        session.setAudioReceiverThread(audioReceiverThread);
                        audioReceiverThread.start();
                        synchronized (this) {
                            wait();
                        }

                        AudioControlServer audioControlServer = new AudioControlServer(this);
                        Thread audioControlServerThread = new Thread(audioControlServer);
                        session.setAudioControlServerThread(audioControlServerThread);
                        audioControlServerThread.start();
                        synchronized (this) {
                            wait();
                        }

                        session.getAirPlay().rtspSetupAudio(new ByteBufOutputStream(response.content()),
                                audioReceiver.getPort(), audioControlServer.getPort());

                        break;

                    case VIDEO:
                        VideoStreamInfo videoStreamInfo = (VideoStreamInfo) mediaStreamInfo;

                        airplayDataConsumer.onVideoFormat(videoStreamInfo);

                        MirroringHandler mirroringHandler = new MirroringHandler(session.getAirPlay(), airplayDataConsumer);
                        MirroringReceiver airPlayReceiver = new MirroringReceiver(airPlayPort, mirroringHandler);
                        Thread airPlayReceiverThread = new Thread(airPlayReceiver);
                        session.setAirPlayReceiverThread(airPlayReceiverThread, airPlayReceiver);
                        airPlayReceiverThread.start();

                        Log.d("RTSPHandler", "New MirroringReceiver thread started with ID: " + airPlayReceiverThread.getId());
                        LogRepository.INSTANCE.addLog("RTSPHandler", "New MirroringReceiver thread started with ID: " + airPlayReceiverThread.getId(), 'I');

                        session.getAirPlay().rtspSetupVideo(new ByteBufOutputStream(response.content()), airPlayPort, airTunesPort, 7011);
                        break;
                }
            }
            LogRepository.INSTANCE.addLog(TAG, "RTSP SETUP response sent", 'I');
            return sendResponse(ctx, request, response);
        } else if (RtspMethods.GET_PARAMETER.equals(request.method())) {
            byte[] content = "volume: 1.000000\r\n".getBytes(StandardCharsets.US_ASCII);
            response.content().writeBytes(content);
            LogRepository.INSTANCE.addLog(TAG, "GET_PARAMETER response sent", 'I');
            return sendResponse(ctx, request, response);
        } else if (RtspMethods.RECORD.equals(request.method())) {
//
//            session.getAirPlay().printPlist("RECORD ",new ByteBufInputStream(request.content()));
//            response.headers().add("Audio-Latency", "11025");
//            response.headers().add("Audio-Jack-Status", "connected; type=analog");
            LogRepository.INSTANCE.addLog(TAG, "RECORD response sent", 'I');
            return sendResponse(ctx, request, response);
        } else if (RtspMethods.SET_PARAMETER.equals(request.method())) {
            session.getAirPlay().printPlist("SET_PARAMETER ",new ByteBufInputStream(request.content()));

            int volume = session.getAirPlay().rtspSetParameterInfo(new ByteBufInputStream(request.content()));
            // todo 设置音量
            LogRepository.INSTANCE.addLog(TAG, "SET_PARAMETER response sent", 'I');
            return sendResponse(ctx, request, response);
        } else if ("FLUSH".equals(request.method().toString())) {
            LogRepository.INSTANCE.addLog(TAG, "FLUSH response sent", 'I');
            return sendResponse(ctx, request, response);
        } else if (RtspMethods.TEARDOWN.equals(request.method())) {
            session.getAirPlay().printPlist("TEARDOWN ",new ByteBufInputStream(request.content()));

            Log.d("RTSPHandler", "TEARDOWN: session.getAirPlay().isPairVerified() = " + session.getAirPlay().isPairVerified());
            Log.d("RTSPHandler", "TEARDOWN: request was " + request.content());
            LogRepository.INSTANCE.addLog(TAG, "TEARDOWN: request was " + request.content(), 'I');

            MediaStreamInfo mediaStreamInfo = session.getAirPlay().rtspGetMediaStreamInfo(new ByteBufInputStream(request.content()), request.method().toString());
            if (mediaStreamInfo != null) {
                switch (mediaStreamInfo.getStreamType()) {
                    case AUDIO:
                        session.stopAudio();
                        workaround(ctx);
                        LogRepository.INSTANCE.addLog(TAG, "Audio session stopped.", 'I');
                        Log.d("RTSPHandler", "Audio session stopped.");
                        break;
                    case VIDEO:
                        session.stopMirroring();
                        workaround(ctx);
                        LogRepository.INSTANCE.addLog(TAG, "Mirroring session stopped.", 'I');
                        Log.d("RTSPHandler", "Mirroring session stopped.");
                        break;
                }
            } else {
                session.stopAudio();
                session.stopMirroring();
                workaround(ctx);
                LogRepository.INSTANCE.addLog(TAG, "Audio and mirroring sessions stopped.", 'I');
                Log.d("RTSPHandler", "Audio and mirroring sessions stopped.");
                LogRepository.INSTANCE.setConnection(false);
                LogRepository.INSTANCE.addLog(TAG, "setConnection(false)", 'I');
            }
            LogRepository.INSTANCE.addLog(TAG, "TEARDOWN response sent", 'I');
            return sendResponse(ctx, request, response);
        } else if ("POST".equals(request.method().toString()) && request.uri().equals("/audioMode")) {
            session.getAirPlay().printPlist("audioMode ",new ByteBufInputStream(request.content()));

            Log.d("RTSPHandler", "POST: session.getAirPlay().isPairVerified() = " + session.getAirPlay().isPairVerified());
            return sendResponse(ctx, request, response);
        }
        return false;
    }

    private void workaround( ChannelHandlerContext ctx) {
        ctx.channel().connect(ctx.channel().remoteAddress());
        LogRepository.INSTANCE.addLog(TAG, "CTX channel connected", 'I');
    }
}
