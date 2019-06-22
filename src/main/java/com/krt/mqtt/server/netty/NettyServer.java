package com.krt.mqtt.server.netty;

import com.krt.mqtt.server.constant.CommonConst;
import com.krt.mqtt.server.thread.AliveThread;
import com.krt.mqtt.server.thread.MessageThread;
import com.krt.mqtt.server.thread.ReplyMessageThread;
import com.krt.mqtt.server.thread.SendMessageThread;
import com.krt.mqtt.server.utils.SignalUtil;
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

            for(int i = 0; i<CommonConst.DEVICE_DATA_THREAD_SIZE; i++) {
                CommonConst.DEVICE_DATA_THREAD_ARRAY[i] = new MessageThread(i);
            }
            new AliveThread();
            new ReplyMessageThread();
            new SendMessageThread();

            SignalUtil.initSignal(new NettyShutdownHandler());

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
