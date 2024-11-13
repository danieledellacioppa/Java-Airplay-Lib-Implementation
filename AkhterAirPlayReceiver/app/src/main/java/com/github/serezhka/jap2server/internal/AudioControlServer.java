package com.github.serezhka.jap2server.internal;

import com.cjx.airplayjavademo.tools.LogRepository;
import com.github.serezhka.jap2server.internal.handler.audio.AudioControlHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;

public class AudioControlServer implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(AudioControlServer.class);

    private final Object monitor;

    private int port;

    public AudioControlServer(Object monitor) {
        this.monitor = monitor;
    }

    @Override
    public void run() {
        Bootstrap bootstrap = new Bootstrap();
        EventLoopGroup workerGroup = eventLoopGroup();
        AudioControlHandler audioControlHandler = new AudioControlHandler();

        try {
            bootstrap
                    .group(workerGroup)
                    .channel(datagramChannelClass())
                    .localAddress(new InetSocketAddress(0)) // bind random port
                    .handler(new ChannelInitializer<DatagramChannel>() {
                        @Override
                        public void initChannel(final DatagramChannel ch) {
                            ch.pipeline().addLast(audioControlHandler);
                        }
                    });

            ChannelFuture channelFuture = bootstrap.bind().sync();

            log.info("Audio control server listening on port: {}",
                    port = ((InetSocketAddress) channelFuture.channel().localAddress()).getPort());
            LogRepository.INSTANCE.addLog("AudioControlServer", "Audio control server listening on port: " + port, 'I');

            synchronized (monitor) {
                monitor.notify();
            }

            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.info("Audio control server interrupted");
            LogRepository.INSTANCE.addLog("AudioControlServer", "Audio control server interrupted.", 'I');
        } finally {
            log.info("Audio control server stopped");
            LogRepository.INSTANCE.addLog("AudioControlServer", "Audio control server stopped.", 'I');
            workerGroup.shutdownGracefully();
        }
    }

    public int getPort() {
        return port;
    }

    private EventLoopGroup eventLoopGroup() {
        return Epoll.isAvailable() ? new EpollEventLoopGroup() : new NioEventLoopGroup();
    }

    private Class<? extends DatagramChannel> datagramChannelClass() {
        return Epoll.isAvailable() ? EpollDatagramChannel.class : NioDatagramChannel.class;
    }
}