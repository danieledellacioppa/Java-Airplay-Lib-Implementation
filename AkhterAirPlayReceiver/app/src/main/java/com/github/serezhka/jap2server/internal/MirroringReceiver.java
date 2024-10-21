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

            channelFuture = serverBootstrap.bind().sync();
            Log.d(TAG, "Mirroring receiver listening on port: " + port);

            // Attende la chiusura del canale
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            Log.e(TAG, "Mirroring receiver interrupted", e);
            Thread.currentThread().interrupt(); // Interrompe il thread corrente
        } finally {
            Log.e(TAG, "Mirroring receiver stopped");
            // Chiudere la connessione in modo sicuro
            if (channelFuture != null && channelFuture.channel().isOpen()) {
                try {
                    channelFuture.channel().close().sync();
                    Log.d(TAG, "Channel closed successfully.");
                } catch (InterruptedException e) {
                    Log.e(TAG, "Failed to close the channel", e);
                    Thread.currentThread().interrupt(); // Interrompe il thread corrente
                }
            }
            // Chiudere i gruppi di eventi
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
