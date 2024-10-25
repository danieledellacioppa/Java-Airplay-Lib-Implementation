package com.github.serezhka.jap2server.internal;

import com.cjx.airplayjavademo.tools.LogRepository;
import com.github.serezhka.jap2server.AirplayDataConsumer;
import com.github.serezhka.jap2server.internal.handler.control.FairPlayHandler;
import com.github.serezhka.jap2server.internal.handler.control.HeartBeatHandler;
import com.github.serezhka.jap2server.internal.handler.control.PairingHandler;
import com.github.serezhka.jap2server.internal.handler.control.RTSPHandler;
import com.github.serezhka.jap2server.internal.handler.mirroring.MirroringHandler;
import com.github.serezhka.jap2server.internal.handler.session.SessionManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.rtsp.RtspDecoder;
import io.netty.handler.codec.rtsp.RtspEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class ControlServer implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(MirroringHandler.class);

    private final PairingHandler pairingHandler;
    private final FairPlayHandler fairPlayHandler;
    private final RTSPHandler rtspHandler;
    private final HeartBeatHandler heartBeatHandler;

    private final int airTunesPort;

    public ControlServer(int airPlayPort, int airTunesPort, AirplayDataConsumer airplayDataConsumer) {
        this.airTunesPort = airTunesPort;
        SessionManager sessionManager = new SessionManager();
        pairingHandler = new PairingHandler(sessionManager);
        fairPlayHandler = new FairPlayHandler(sessionManager);
        rtspHandler = new RTSPHandler(airPlayPort, airTunesPort, sessionManager, airplayDataConsumer);
        heartBeatHandler = new HeartBeatHandler(sessionManager);
    }

    @Override
    public void run() {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        EventLoopGroup bossGroup = eventLoopGroup();
        EventLoopGroup workerGroup = eventLoopGroup();
        try {
            serverBootstrap
                    .group(bossGroup, workerGroup)
                    .channel(serverSocketChannelClass())
                    .localAddress(new InetSocketAddress(airTunesPort))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(final SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(
                                    // Aggiungi IdleStateHandler per gestire timeout di inattività
                                    new IdleStateHandler(0, 0, 3), // Timeout di 3 secondi di inattività totale
                                    new ChannelDuplexHandler() {
                                        @Override
                                        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                                            if (evt instanceof IdleStateEvent) {
                                                IdleStateEvent event = (IdleStateEvent) evt;
                                                if (event.state() == IdleState.ALL_IDLE) {
                                                    log.info("Connection idle for 3 seconds. Closing connection.");
                                                    LogRepository.INSTANCE.addLog("ControlServer", "Connection idle for 3 seconds. Closing connection.");
                                                    ctx.close(); // Chiudi la connessione inattiva
                                                }
                                            }
                                            super.userEventTriggered(ctx, evt);
                                        }
                                    },
                                    new RtspDecoder(),
                                    new RtspEncoder(),
                                    new HttpObjectAggregator(64 * 1024),
                                    new LoggingHandler(LogLevel.DEBUG),
                                    pairingHandler,
                                    fairPlayHandler,
                                    rtspHandler,
                                    heartBeatHandler);
                        }
                    })
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_REUSEADDR, true)
            // Rimuoviamo SO_KEEPALIVE per evitare di mantenere la connessione aperta
            //.childOption(ChannelOption.SO_KEEPALIVE, true); // Rimosso

            // chiudiamo la connessione se non ci sono dati in arrivo
            .childOption(ChannelOption.AUTO_CLOSE, true)
            .childOption(ChannelOption.SO_LINGER, 0);


            ChannelFuture channelFuture = serverBootstrap.bind().sync();
            log.info("Control server listening on port: {}", airTunesPort);
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            log.info("Control server stopped");
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private EventLoopGroup eventLoopGroup() {
        return Epoll.isAvailable() ? new EpollEventLoopGroup() : new NioEventLoopGroup();
    }

    private Class<? extends ServerSocketChannel> serverSocketChannelClass() {
        return Epoll.isAvailable() ? EpollServerSocketChannel.class : NioServerSocketChannel.class;
    }
}