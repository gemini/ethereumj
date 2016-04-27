package org.ethereum.net.server;

import io.netty.channel.*;
import org.ethereum.config.SystemProperties;
import org.ethereum.listener.EthereumListener;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

/**
 * This class establishes a listener for incoming connections.
 * See <a href="http://netty.io">http://netty.io</a>.
 *
 * @author Roman Mandeleil
 * @since 01.11.2014
 */
@Component
public class PeerServer {

    private static final Logger logger = LoggerFactory.getLogger("net");

    @Autowired
    SystemProperties config;

    @Autowired
    private ApplicationContext ctx;

    public EthereumChannelInitializer ethereumChannelInitializer;

    @Autowired
    EthereumListener ethereumListener;

    private boolean listening;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private io.netty.channel.Channel channel;
    
    public PeerServer() {
    }

    public void start(int port) {

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ethereumChannelInitializer = ctx.getBean(EthereumChannelInitializer.class, "");

        ethereumListener.trace("Listening on port " + port);


        try {
            ServerBootstrap b = new ServerBootstrap();

            b.group(bossGroup, workerGroup);
            b.channel(NioServerSocketChannel.class);

            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT);
            b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.peerConnectionTimeout());

            b.handler(new LoggingHandler());
            b.childHandler(ethereumChannelInitializer);

            // Start the client.
            logger.info("Listening for incoming connections, port: [{}] ", port);
            logger.info("NodeId: [{}] ", Hex.toHexString(config.nodeId()));

            channel = b.bind(port).sync().channel();

            listening = true;
            // Wait until the connection is closed.
            channel.closeFuture().sync();
            logger.debug("Connection is closed");

            // TODO review listening use
            listening = false;
        } catch (Exception e) {
            logger.debug("Exception: {} ({})", e.getMessage(), e.getClass().getName());
            throw new Error("Server Disconnected");
        } finally {
            workerGroup.shutdownGracefully();

        }
    }

    public boolean isListening() {
        return listening;
    }

    @PreDestroy
    public void destroy() {
        if (listening) {
            logger.info("destroy(): set listening to false");
            listening = false;
            logger.info("destroy(): shutting down the PeerServer channel");
            try {
                channel.close().sync();
            }
            catch (Exception e) {
                logger.warn("destroy(): failed to shut down the PeerServer channel because: "+e.getMessage(), e);
            }
            logger.info("destroy(): shutting down worker group");
            try {
                workerGroup.shutdownGracefully();
            }
            catch (Exception e) {
                logger.warn("destroy(): failed to shut down worker group because: "+e.getMessage(), e);
            }
            logger.info("destroy(): shutting down boss group");
            try {
                bossGroup.shutdownGracefully();
            }
            catch (Exception e) {
                logger.warn("destroy(): failed to shut down boss group because: "+e.getMessage(), e);
            }
        }
    }
}
