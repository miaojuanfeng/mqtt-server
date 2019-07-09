package com.krt.mqtt.server.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

@Component
public class NettyServerInitializer extends ChannelInitializer<SocketChannel> {

    private SslContext sslContext;

    @Autowired
    private NettyServerHandler nettyServerHandler;

    public void setSslContext(SslContext sslContext) {
        this.sslContext = sslContext;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        SSLEngine engine = sslContext.newEngine(socketChannel.alloc());
        engine.setUseClientMode(false);
        ChannelPipeline channelPipeline = socketChannel.pipeline();
        channelPipeline.addFirst("ssl", new SslHandler(engine));
        channelPipeline.addLast("decoder", new MqttDecoder());
        channelPipeline.addLast("encoder", MqttEncoder.INSTANCE);
        channelPipeline.addLast("handler", nettyServerHandler);
    }
}
