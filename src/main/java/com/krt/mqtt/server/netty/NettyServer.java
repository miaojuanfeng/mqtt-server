package com.krt.mqtt.server.netty;

import com.krt.mqtt.server.thread.AliveThread;
import com.krt.mqtt.server.thread.ReplyMessageThread;
import com.krt.mqtt.server.thread.SendMessageThread;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NettyServer {

    private static Integer port;

    @Value("${server.port}")
    public void setPort(Integer port){
        this.port = port;
    }

    public static void start(){
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new NettyServerInitializer())
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            AliveThread aliveThread = new AliveThread();
            aliveThread.start();
            ReplyMessageThread replyMessageThread = new ReplyMessageThread();
            replyMessageThread.start();
            SendMessageThread sendMessageThread = new SendMessageThread();
            sendMessageThread.start();

            ChannelFuture channelFuture = serverBootstrap.bind(port).sync();

            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }

    }
}
