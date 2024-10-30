package com.github.serezhka.jap2server.internal;

import android.util.Log;

import com.cjx.airplayjavademo.tools.LogRepository;
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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

/**
 * <h1 style="color: #2e6c80;">MirroringReceiver</h1>
 *
 * <p style="font-size: 1.1em; color: #555555;">
 * The <strong>MirroringReceiver</strong> class is responsible for setting up and running an AirPlay mirroring receiver
 * on a specified port. It leverages Netty's event-driven architecture to manage incoming connections and
 * handle mirroring data through a custom <strong>MirroringHandler</strong>. The receiver listens for video and audio
 * data and processes them via the provided handler.
 * </p>
 *
 * <h2 style="color: #3a79a1;">Main Features</h2>
 * <ul>
 *   <li>Starts a Netty server using <strong>Epoll</strong> or <strong>Nio</strong>, depending on system capabilities.</li>
 *   <li>Handles incoming video/audio streams by adding a <strong>MirroringHandler</strong> to the pipeline.</li>
 *   <li>Supports configurable reconnection logic with retry attempts in case of connection failure.</li>
 *   <li>Ensures resources (channels and event loop groups) are properly cleaned up after stopping.</li>
 * </ul>
 *
 * <h2 style="color: #3a79a1;">Constructor Parameters</h2>
 * <ul>
 *   <li><strong>port</strong>: The port number on which the server should listen for incoming connections.</li>
 *   <li><strong>mirroringHandler</strong>: The custom handler that processes the incoming mirroring data.</li>
 * </ul>
 *
 * <h2 style="color: #3a79a1;">Methods</h2>
 * <ul>
 *   <li><strong>run()</strong>: Starts the server, handles the connection lifecycle, and manages reconnection logic.</li>
 *   <li><strong>handleReconnection()</strong>: Manages retry attempts for reconnection in case of failure.</li>
 *   <li><strong>closeChannel(ChannelFuture)</strong>: Closes the channel safely to avoid resource leaks.</li>
 *   <li><strong>eventLoopGroup()</strong>: Returns an appropriate <strong>EventLoopGroup</strong> (Epoll or Nio) based on system availability.</li>
 *   <li><strong>serverSocketChannelClass()</strong>: Returns the appropriate server socket channel class (Epoll or Nio).</li>
 * </ul>
 *
 * <h2 style="color: #3a79a1;">Usage</h2>
 * <p style="font-size: 1.1em; color: #555555;">
 * This class is designed to be used as the core of an AirPlay mirroring receiver. It listens for incoming connections, processes
 * video and audio data, and manages reconnections if the connection is lost.
 * </p>
 */
public class MirroringReceiver implements Runnable {
    private final long threadID;  // ID univoco per ogni istanza

    private final int port;
    private final MirroringHandler mirroringHandler;
    private static final String TAG = "MirroringReceiver";

    public MirroringReceiver(int port, MirroringHandler mirroringHandler) {
        this.port = port;
        this.mirroringHandler = mirroringHandler;
        this.threadID = Thread.currentThread().getId();
    }

    @Override
    public void run() {
        Log.d(TAG, "Starting mirroring receiver instance: " + threadID);
        LogRepository.INSTANCE.addLog(TAG, "Starting mirroring receiver instance: " + threadID);

        EventLoopGroup bossGroup = eventLoopGroup();
        EventLoopGroup workerGroup = eventLoopGroup();
        ServerBootstrap serverBootstrap = new ServerBootstrap();
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
            Log.d(TAG, "ServerBootstrap configured for mirroring receiver " + threadID);
            LogRepository.INSTANCE.addLog(TAG, "ServerBootstrap configured for mirroring receiver " + threadID);
        } catch (Exception e) {
            Log.e(TAG, "Error during ServerBootstrap configuration for mirroring receiver " + threadID, e);
            return;  // Termina l'esecuzione se c'è un errore di configurazione
        }

        // Blocco 2: Avvio del binding del server
        try {

            if (!isPortAvailable(port)) {
                Log.e(TAG, "Port " + port + " is already in use. Exiting...");
                LogRepository.INSTANCE.addLog(TAG, "Port " + port + " is already in use. Exiting...");
                return;
            }

            // Avvia il server e attende la chiusura del canale
            channelFuture = serverBootstrap.bind().sync();
            Log.d(TAG, "Mirroring receiver" + threadID + " started on port: " + port);
            LogRepository.INSTANCE.addLog(TAG, "Mirroring receiver" + threadID + " started on port: " + port);
        } catch (InterruptedException e) {
            Log.e(TAG, "Mirroring receiver " + threadID + " interrupted during bind", e);
            Thread.currentThread().interrupt();
            return;
        } catch (Exception e) {
            Log.e(TAG, "Error during bind for mirroring receiver " + threadID, e);
            return;
        }

        // Blocco 3: Attesa della chiusura del canale
        try {
            // Attende che il canale si chiuda
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            Log.e(TAG, "Mirroring receiver" + threadID + " interrupted during setup", e);
            Thread.currentThread().interrupt();
            LogRepository.INSTANCE.setConnection(false);
            System.gc();
            } catch (Exception e) {
                Log.e(TAG, "Error starting mirroring receiver" + threadID, e);
            } finally {
            Log.e(TAG, "Mirroring receiver" + threadID + " shutting down...");
            closeChannel(channelFuture);
            // Chiusura sincrona dei gruppi di eventi con ritardo per liberare risorse
            bossGroup.shutdownGracefully().syncUninterruptibly();
            workerGroup.shutdownGracefully().syncUninterruptibly();

            Log.d(TAG, "EventLoopGroups for mirroring receiver " + threadID + " have been shut down.");
        }

    }

    // Metodo per controllare se la porta è libera
    private boolean isPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // Metodo per chiudere il canale in modo sicuro
    private void closeChannel(ChannelFuture channelFuture) {
        if (channelFuture != null && channelFuture.channel().isOpen()) {
            try {
                channelFuture.channel().close().sync();
                Log.d(TAG, "Channel closed successfully "+ threadID);
            } catch (InterruptedException e) {
                Log.e(TAG, "Failed to close the channel "+ threadID, e);
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