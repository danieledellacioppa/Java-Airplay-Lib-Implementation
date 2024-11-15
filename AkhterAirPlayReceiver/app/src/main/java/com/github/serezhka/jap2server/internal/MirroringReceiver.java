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
    private volatile boolean running = true; // Flag per controllare lo stato di esecuzione
    private ChannelFuture channelFuture;
    private static final String TAG = "MirroringReceiver";
    private static final int MAX_RETRIES = 5;

    public MirroringReceiver(int port, MirroringHandler mirroringHandler) {
        this.port = port;
        this.mirroringHandler = mirroringHandler;
        this.threadID = Thread.currentThread().getId();
    }

    @Override
    public void run() {
        LogRepository.INSTANCE.addLog(TAG, "Starting mirroring receiver instance: " + threadID, 'I');

        EventLoopGroup bossGroup = eventLoopGroup();
        EventLoopGroup workerGroup = eventLoopGroup();
        ServerBootstrap serverBootstrap = new ServerBootstrap();

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
            LogRepository.INSTANCE.addLog(TAG, "ServerBootstrap configured for mirroring receiver " + threadID,'I');
        } catch (Exception e) {
//            Log.e(TAG, "Error during ServerBootstrap configuration for mirroring receiver " + threadID, e);
            LogRepository.INSTANCE.addLog(TAG, "Error during ServerBootstrap configuration for mirroring receiver " + threadID, 'E');
            return;  // Termina l'esecuzione se c'è un errore di configurazione
        }

        // Blocco 2: Avvio del binding del server
        try {
                if (!isPortAvailable(port, MAX_RETRIES)) {
                    Log.e(TAG, "Port " + port + " is already in use. Exiting...");
                    LogRepository.INSTANCE.addLog(TAG, "Port " + port + " is already in use. Exiting...", 'E');
                    return;
                }

                // Avvia il server e attende la chiusura del canale
                if (running){
                    channelFuture = serverBootstrap.bind().sync();
//                    Log.d(TAG, "Mirroring receiver threadID: " + threadID + " started on port: " + port);
                    LogRepository.INSTANCE.addLog(TAG, "Mirroring receiver threadID: " + threadID + " started on port: " + port, 'I');
                }

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
            if (running){
                channelFuture.channel().closeFuture().sync();
                LogRepository.INSTANCE.addLog(TAG, "Mirroring receiver:[" + threadID + "] channel closed", 'I');
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Mirroring receiver" + threadID + " interrupted during setup", e);
            Thread.currentThread().interrupt();
            System.gc();
        } catch (Exception e) {
                LogRepository.INSTANCE.addLog(TAG, "Error starting mirroring receiver" + threadID, 'E');
        } finally {
            LogRepository.INSTANCE.setConnection(false);
//            Log.w(TAG, "Mirroring receiver" + threadID + " shutting down...");
//            closeChannel();
//            // Chiusura sincrona dei gruppi di eventi con ritardo per liberare risorse
            bossGroup.shutdownGracefully().syncUninterruptibly();
            workerGroup.shutdownGracefully().syncUninterruptibly();

            LogRepository.INSTANCE.addLog(TAG, "EventLoopGroups for mirroring receiver " + threadID + " have been shut down.", 'I');
        }

    }

    public void shutdown() {
        running = false;  // Interrompe il ciclo `sync` ordinatamente
        closeChannel();   // Chiude il canale in modo sicuro
        LogRepository.INSTANCE.addLog(TAG, "Shutdown initiated for MirroringReceiver.", 'I');

        // Log per verificare lo stato dei gruppi di eventi
        if (channelFuture != null && !channelFuture.channel().isOpen()) {
            LogRepository.INSTANCE.addLog(TAG, "Channel for MirroringReceiver " + threadID + " is closed.", 'I');
        } else {
            LogRepository.INSTANCE.addLog(TAG, "Channel for MirroringReceiver " + threadID + " did not close properly.", 'E');
        }
    }


    /**
     * This method checks if the specified port is available for binding.
     * It retries the check up to the specified number of times.
     * @param port
     * @param maxRetries
     * @return
     */
    private boolean isPortAvailable(int port, int maxRetries) {
        for (int i = 0; i < maxRetries; i++) {
            LogRepository.INSTANCE.addLog(TAG, "Checking port availability for mirroring receiver " + threadID, 'I');
            LogRepository.INSTANCE.addLog(TAG, "Attempt " + (i + 1) + " of " + maxRetries, 'I');
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                serverSocket.setReuseAddress(true);
                LogRepository.INSTANCE.addLog(TAG, "Port " + port + " is available.", 'I');
                return true;
            } catch (IOException e) {
                try {
                    LogRepository.INSTANCE.addLog(TAG, "Port " + port + " is not available. Retrying...", 'W');
                    Thread.sleep(100); // Delay before retrying
                } catch (InterruptedException ie) {
                    LogRepository.INSTANCE.addLog(TAG, "Interrupted while waiting to retry port check", 'E');
                    Thread.currentThread().interrupt();
                }
            }
        }
        LogRepository.INSTANCE.addLog(TAG, "Port " + port + " is not available after " + maxRetries + " retries.", 'E');
        return false;
    }


    // Metodo per chiudere il canale in modo sicuro

    /**
     * questa funzione FORSE dovrebbe tenere il canale aperto per evitare la perdita di risorse nello stile seguente:
     *
     *     private void workaround( ChannelHandlerContext ctx) {
     *         ctx.channel().connect(ctx.channel().remoteAddress());
     *         LogRepository.INSTANCE.addLog(TAG, "CTX channel connected", 'I');
     *     }
     */
//    private void closeChannel() {
//        if (channelFuture != null && channelFuture.channel().isOpen()) {
//            try {
////                channelFuture.channel().close().sync();
//
//                channelFuture.channel().connect(channelFuture.channel().remoteAddress());
//
//                LogRepository.INSTANCE.addLog(TAG, "channelFuture.channel() is connected", 'I');
//            }
//            catch (Exception e) {
//                LogRepository.INSTANCE.addLog(TAG, "channelFuture.channel() failed to connect", 'E');
//            }
////            catch (InterruptedException e) {
////                LogRepository.INSTANCE.addLog(TAG, "Failed to close the channel "+ threadID, 'E');
////                Thread.currentThread().interrupt();
////            }
//        } else {
//            LogRepository.INSTANCE.addLog(TAG, "Channel already closed or not open "+ threadID, 'W');
//        }
//    }

    private void closeChannel() {
        if (channelFuture != null && channelFuture.channel().isOpen()) {
            try {
                channelFuture.channel().close().sync();

                LogRepository.INSTANCE.addLog(TAG, "channelFuture.channel() is connected", 'I');
            }
            catch (InterruptedException e) {
                LogRepository.INSTANCE.addLog(TAG, "Failed to close the channel "+ threadID, 'E');
                Thread.currentThread().interrupt();
            }
        } else {
            LogRepository.INSTANCE.addLog(TAG, "Channel already closed or not open "+ threadID, 'W');
        }
    }

    private EventLoopGroup eventLoopGroup() {
        return Epoll.isAvailable() ? new EpollEventLoopGroup() : new NioEventLoopGroup();
    }

    private Class<? extends ServerSocketChannel> serverSocketChannelClass() {
        return Epoll.isAvailable() ? EpollServerSocketChannel.class : NioServerSocketChannel.class;
    }
}