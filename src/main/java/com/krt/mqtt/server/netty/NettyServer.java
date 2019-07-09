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
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

@Component
@Slf4j
public class NettyServer {

    @Autowired
    private NettyServerInitializer nettyServerInitializer;

    @Value("${server.port}")
    private Integer port;

    @Value("${server.jks.path}")
    private String jksPath;

    @Value("${server.jks.password}")
    private String jksPassword;

    public void start(){
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            KeyManagerFactory keyManagerFactory = null;
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(new FileInputStream(jksPath), jksPassword.toCharArray());
            keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keyStore, jksPassword.toCharArray());
            SslContext sslContext = SslContextBuilder.forServer(keyManagerFactory).build();
            nettyServerInitializer.setSslContext(sslContext);

            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childHandler(nettyServerInitializer)
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
        } catch (Exception e) {
            log.info("服务器初始化失败："+e.getMessage());
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }

    }
}
