package com.github.serezhka.jap2server.internal;

import android.util.Log;
import com.github.serezhka.jap2server.internal.handler.mirroring.MirroringHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

public class MirroringReceiver implements Runnable {

    private final int port;
    private final MirroringHandler mirroringHandler;
    private static final String TAG = "MirroringReceiver";

    // Aggiungi una variabile per il numero massimo di tentativi di riconnessione
    private final int maxRetryAttempts = 5;
    private int retryAttempts = 0;

    public MirroringReceiver(int port, MirroringHandler mirroringHandler) {
        this.port = port;
        this.mirroringHandler = mirroringHandler;
    }

    @Override
    public void run() {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        EventLoopGroup bossGroup = eventLoopGroup();
        EventLoopGroup workerGroup = eventLoopGroup();
        ChannelFuture channelFuture = null;

        try {
            do {
                try {
                    serverBootstrap
                            .group(bossGroup, workerGroup)
                            .channel(serverSocketChannelClass())
                            .localAddress(new InetSocketAddress(port))
                            .childHandler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                public void initChannel(final SocketChannel ch) {
                                    ch.pipeline().addLast(mirroringHandler);
                                }
                            })
                            .childOption(ChannelOption.TCP_NODELAY, true)
                            .childOption(ChannelOption.SO_REUSEADDR, true)
                            .childOption(ChannelOption.SO_KEEPALIVE, true);

                    // Avvia il server e attende la chiusura del canale
                    channelFuture = serverBootstrap.bind().sync();
                    Log.d(TAG, "Mirroring receiver listening on port: " + port);

                    // Attende che il canale si chiuda
                    channelFuture.channel().closeFuture().sync();
                    break; // Esce dal ciclo se tutto va bene

                } catch (InterruptedException e) {
                    Log.e(TAG, "Mirroring receiver interrupted", e);
                    Thread.currentThread().interrupt(); // Interrompe il thread corrente
                }
            } while (retryAttempts++ < maxRetryAttempts && handleReconnection());

        } finally {
            Log.e(TAG, "Mirroring receiver stopped");
            closeChannel(channelFuture);
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    // Gestione della riconnessione automatica
    private boolean handleReconnection() {
        Log.w(TAG, "Attempting to reconnect... (" + retryAttempts + "/" + maxRetryAttempts + ")");
        try {
            Thread.sleep(2000); // Attende 2 secondi prima di ritentare
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    // Metodo per chiudere il canale in modo sicuro
    private void closeChannel(ChannelFuture channelFuture) {
        if (channelFuture != null && channelFuture.channel().isOpen()) {
            try {
                channelFuture.channel().close().sync();
                Log.d(TAG, "Channel closed successfully.");
            } catch (InterruptedException e) {
                Log.e(TAG, "Failed to close the channel", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    private EventLoopGroup eventLoopGroup() {
        return Epoll.isAvailable() ? new EpollEventLoopGroup() : new NioEventLoopGroup();
    }

    private Class<? extends ServerSocketChannel> serverSocketChannelClass() {
        return Epoll.isAvailable() ? EpollServerSocketChannel.class : NioServerSocketChannel.class;
    }
}