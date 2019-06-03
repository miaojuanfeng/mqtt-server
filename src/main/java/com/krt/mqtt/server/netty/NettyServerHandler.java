package com.krt.mqtt.server.netty;

import com.krt.mqtt.server.utils.SpringUtil;
import io.netty.channel.*;
import io.netty.handler.codec.mqtt.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@ChannelHandler.Sharable
@Slf4j
@Component
public class NettyServerHandler extends SimpleChannelInboundHandler<MqttMessage> {

    @Autowired
    private MqttMessageService mqttMessageService;

    @Autowired
    private MqttChannelApi mqttChannelApi;

    @Autowired
    private MqttMessageApi mqttMessageApi;

    public NettyServerHandler(){
        mqttMessageService = SpringUtil.getBean(MqttMessageService.class);
        mqttChannelApi = SpringUtil.getBean(MqttChannelApi.class);
        mqttMessageApi = SpringUtil.getBean(MqttMessageApi.class);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage mqttMessage) throws Exception {
        /**
         * 客户端到服务端的网络连接建立后，客户端发送给服务端的第一个报文必须是CONNECT报文
         * 否则断开与该客户端的链接
         */
        if( !mqttMessage.fixedHeader().messageType().equals(MqttMessageType.CONNECT) && !mqttChannelApi.checkLogin(ctx) ){
            /**
             * 如果客户端在未登录成功的状态下发送CONNECT以外的报文，
             * 该行为违反协议规定，服务器立即断开与该客户端的连接
             */
            ctx.channel().close();
            return;
        }
        /**
         * 输出报文日志
         */
        String deviceId = "";
        if( mqttChannelApi.hasAttr(ctx, MqttChannelApi._deviceId) ) {
            deviceId = mqttChannelApi.getChannelDeviceId(ctx);
        }else{
            deviceId = ((MqttConnectMessage) mqttMessage).payload().clientIdentifier();
        }
        log.info("客户端（" + deviceId + "）发来报文: " + mqttMessage);
        /**
         * 处理客户端连接报文
         */
        if( mqttMessage.fixedHeader().messageType().equals(MqttMessageType.CONNECT) ){
            mqttMessageService.replyConnectMessage(ctx, (MqttConnectMessage) mqttMessage);
            return;
        }
        /**
         * 收到客户端发来的任何报文，包括但不限于PINGREQ，
         * 则证明客户端存活，需要更新客户端活跃时间
         */
        mqttChannelApi.updateActiveTime(ctx);
        /**
         * 处理客户端发来的其他类型报文
         */
        switch (mqttMessage.fixedHeader().messageType()){
            case DISCONNECT:
                mqttMessageService.replyDisConnectMessage(ctx);
                break;
            case PINGREQ:
                mqttMessageApi.PINGRESP(ctx);
                break;
            case PUBLISH:
                mqttMessageService.replyPublishMessage(ctx, (MqttPublishMessage) mqttMessage);
                break;
            case PUBACK:
                mqttMessageService.replyPubAckMessage(ctx, (MqttPubAckMessage) mqttMessage);
                break;
            case PUBREC:
                mqttMessageService.replyPubRecMessage(ctx, (MqttPubAckMessage) mqttMessage);
                break;
            case PUBREL:
                mqttMessageService.replyPubRelMessage(ctx, mqttMessage);
                break;
            case PUBCOMP:
                mqttMessageService.replyPubCompMessage(ctx, mqttMessage);
                break;
            case SUBSCRIBE:
                mqttMessageService.replySubscribeMessage(ctx, (MqttSubscribeMessage) mqttMessage);
                break;
            case UNSUBSCRIBE:
                mqttMessageService.replyUnsubscribeMessage(ctx, (MqttUnsubscribeMessage) mqttMessage);
                break;
        }
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
//        System.out.println("channel注册");
        super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
//        System.out.println("channel注销");
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
//        System.out.println("客户端与服务端成功连接");
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
//        System.out.println("客户端与服务端断开连接");
//        /**
//         *  该事件与异常事件同时触发，会关闭2次通道
//          */
//        mqttMessageService.sendWillMessage(ctx);
//        mqttMessageService.forceClose(ctx);
        super.channelInactive(ctx);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
//        System.out.println("channel读取数据完毕");
        super.channelReadComplete(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
//        System.out.println("用户事件触发");
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
//        System.out.println("channel可写事件更改");
        super.channelWritabilityChanged(ctx);
    }

    @Override
    //channel发生异常，若不关闭，随着异常channel的逐渐增多，性能也就随之下降
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("捕获通道异常: "+cause);
        cause.printStackTrace();
        mqttMessageService.sendWillMessage(ctx);
        mqttChannelApi.closeChannel(ctx);
//        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
//        System.out.println("助手类添加");
        super.handlerAdded(ctx);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
//        System.out.println("助手类移除");
        super.handlerRemoved(ctx);
    }
}
