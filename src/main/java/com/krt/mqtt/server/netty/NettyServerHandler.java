package com.krt.mqtt.server.netty;

import com.krt.mqtt.server.service.UserService;
import com.krt.mqtt.server.utils.SpringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.*;
import io.netty.handler.codec.mqtt.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.ByteBuffer;

@ChannelHandler.Sharable
@Slf4j
@Component
public class NettyServerHandler extends SimpleChannelInboundHandler<MqttMessage> {

    @Autowired
    private MqttMessageService mqttMessageService;

    public NettyServerHandler(){
        mqttMessageService = SpringUtil.getBean(MqttMessageService.class);
    }

//    static {
//        userService = SpringUtil.getBean(UserService.class);
//    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage mqttMessage) throws Exception {
        System.out.println("Client said:" + mqttMessage);
        System.out.println("mqttMessageService:" + mqttMessageService);
        /**
         * 客户端到服务端的网络连接建立后，客户端发送给服务端的第一个报文必须是CONNECT报文
         * 否则断开与该客户端的链接
         */
        if( !mqttMessage.fixedHeader().messageType().equals(MqttMessageType.CONNECT) && !MqttMessageService.checkLogin(ctx) ){
            ctx.channel().close();
            return;
        }
        if( mqttMessage.fixedHeader().messageType().equals(MqttMessageType.CONNECT) ){
//            MqttMessageService.replyConnectMessage(ctx, (MqttConnectMessage) mqttMessage);
            mqttMessageService.replyConnectMessage(ctx, (MqttConnectMessage) mqttMessage);
            return;
        }
        /**
         * 更新客户端活跃时间
         */
        MqttMessageService.updateActiveTime(ctx);
        //
        switch (mqttMessage.fixedHeader().messageType()){
//            case CONNECT:
//                MqttMessageService.replyConnectMessage(ctx, (MqttConnectMessage) mqttMessage);
//                break;
            case DISCONNECT:
                MqttMessageService.replyDisConnectMessage(ctx);
                break;
            case PINGREQ:
                MqttMessageService.replyPingReqMessage(ctx);
                break;
            case PUBLISH:
//                ByteBuf byteBuf = ((MqttPublishMessage)mqttMessage).payload();
//                byte[] b = new byte[byteBuf.readableBytes()];
//                byteBuf.readBytes(b);
//                System.out.println(new String(b));
                MqttMessageService.replyPublishMessage(ctx, (MqttPublishMessage) mqttMessage);
                break;
            case PUBREL:
                MqttMessageService.replyPubRelMessage(ctx, mqttMessage);
                break;
            case SUBSCRIBE:
                MqttMessageService.replySubscribeMessage(ctx, (MqttSubscribeMessage) mqttMessage);
                break;
        }
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channel注册");
        super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channel注册");
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channel活跃状态");
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("客户端与服务端断开连接之后");
        super.channelInactive(ctx);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channel读取数据完毕");
        super.channelReadComplete(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        System.out.println("用户事件触发");
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channel可写事件更改");
        super.channelWritabilityChanged(ctx);
    }

    @Override
    //channel发生异常，若不关闭，随着异常channel的逐渐增多，性能也就随之下降
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("捕获channel异常");
        MqttMessageService.sendWillMessage(ctx);
        MqttMessageService.forceClose(ctx);
//        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        System.out.println("助手类添加");
        super.handlerAdded(ctx);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        System.out.println("助手类移除");
        super.handlerRemoved(ctx);
    }
}
