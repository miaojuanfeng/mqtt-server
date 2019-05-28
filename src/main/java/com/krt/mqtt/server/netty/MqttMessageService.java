package com.krt.mqtt.server.netty;

import com.krt.mqtt.server.beans.MqttChannel;
import com.krt.mqtt.server.beans.MqttTopic;
import com.krt.mqtt.server.beans.MqttWill;
import com.krt.mqtt.server.service.UserService;
import com.krt.mqtt.server.utils.SpringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.AttributeKey;
import io.netty.util.IllegalReferenceCountException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class MqttMessageService {

    @Autowired
    private UserService userService;

//    static {
//        userService = SpringUtil.getBean(UserService.class);
//    }

    private static final AttributeKey<Boolean> _login = AttributeKey.valueOf("login");

    private static final AttributeKey<String> _deviceId = AttributeKey.valueOf("deviceId");

    /**
     * 已连接到服务器端的通道
     */
    private static ConcurrentHashMap<String, MqttChannel> channels = new ConcurrentHashMap<>();

    /**
     * 所有主题列表，以及所有订阅该主题的通道
     */
    private static ConcurrentHashMap<String, Set<MqttTopic>> topics = new ConcurrentHashMap<>();

//    /**
//     * 未完成的消息
//     */
//    private static ConcurrentHashMap<Integer, MqttSendMessage> messages = new ConcurrentHashMap<>();

    public void replyConnectMessage(ChannelHandlerContext ctx, MqttConnectMessage mqttConnectMessage){
        log.info("userService: "+userService);
        Channel channel = ctx.channel();
        /**
         * 这里还要看下协议英文原文
         * Mqtt协议规定，相同Client ID客户端已连接到服务器，先前客户端必须断开连接后，服务器才能完成新的客户端CONNECT连接
         */
        String deviceId = mqttConnectMessage.payload().clientIdentifier();
//        System.out.println(deviceId);
        /**
         * 检查是否有重复连接的客户端
         */
        MqttChannel existChannel = channels.get(deviceId);
        if( existChannel != null ){
            System.out.println("exist channel");
            sendDisConnectMessage(existChannel.getCtx(), mqttConnectMessage);
            existChannel.getCtx().channel().close();
        }
        /**
         * 创建一个新的客户端实例
         */
        MqttChannel mqttChannel = new MqttChannel();
        mqttChannel.setDeviceId(deviceId);
        mqttChannel.setCtx(ctx);
        mqttChannel.setKeepAlive(mqttConnectMessage.variableHeader().keepAliveTimeSeconds());
        mqttChannel.setActiveTime(new Date().getTime());
        /**
         * 客户端开启了遗愿消息
         */
        if( mqttConnectMessage.variableHeader().isWillFlag() ){
            MqttWill mqttWill = new MqttWill();
            mqttWill.setWillTopic(mqttConnectMessage.payload().willTopic());
            mqttWill.setWillMessage(mqttConnectMessage.payload().willMessageInBytes());
            mqttWill.setMqttQoS(MqttQoS.valueOf(mqttConnectMessage.variableHeader().willQos()));
            mqttWill.setRetain(mqttConnectMessage.variableHeader().isWillRetain());
            mqttChannel.setMqttWill(mqttWill);
        }
        channel.attr(_login).set(true);
        channel.attr(_deviceId).set(deviceId);
        channels.put(deviceId, mqttChannel);

//        System.out.println(channel.attr(_deviceId).get());

        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.CONNACK, mqttConnectMessage.fixedHeader().isDup(), MqttQoS.AT_MOST_ONCE, mqttConnectMessage.fixedHeader().isRetain(), 0x02);
        MqttConnAckVariableHeader mqttConnAckVariableHeader = new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_ACCEPTED, false);
        MqttConnAckMessage connAck = new MqttConnAckMessage(mqttFixedHeader, mqttConnAckVariableHeader);
        writeAndFlush(ctx, connAck);
    }

    public static void replyPingReqMessage(ChannelHandlerContext ctx){
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttMessage pingResp = new MqttMessage(mqttFixedHeader);
        writeAndFlush(ctx, pingResp);
    }

    public static void updateActiveTime(ChannelHandlerContext ctx){
        String deviceId = ctx.channel().attr(_deviceId).get();
        MqttChannel mqttChannel = channels.get(deviceId);
        mqttChannel.setActiveTime(new Date().getTime());
    }

    public static void replyPublishMessage(ChannelHandlerContext ctx, MqttPublishMessage mqttPublishMessage){
//        ByteBuf byteBuffer = mqttPublishMessage.payload();
//        byte[] bytes = new byte[byteBuffer.readableBytes()];
//        byteBuffer.readBytes(bytes);
//        String content = new String(bytes);
//        System.out.println("publish name: "+mqttPublishMessage.variableHeader().topicName());
//        System.out.println("publish content: "+content);
        MqttQoS qosLevel = mqttPublishMessage.fixedHeader().qosLevel();
        ByteBuf b = mqttPublishMessage.payload();
        byte[] payload = new byte[b.readableBytes()];
        b.readBytes(payload);
        switch (qosLevel){
            case AT_MOST_ONCE:
                pushPublishTopic(ctx, mqttPublishMessage);
                break;
            case AT_LEAST_ONCE:
//                saveMessage(ctx,
//                        mqttPublishMessage.variableHeader().messageId(),
//                        mqttPublishMessage.variableHeader().topicName(),
//                        payload,
//                        qosLevel,
//                        MqttMessageStateConst.PUB);
                sendPubAckMessage(ctx, mqttPublishMessage);
                pushPublishTopic(ctx, mqttPublishMessage);
                break;
            case EXACTLY_ONCE:
//                saveMessage(ctx,
//                        mqttPublishMessage.variableHeader().messageId(),
//                        mqttPublishMessage.variableHeader().topicName(),
//                        payload,
//                        qosLevel,
//                        MqttMessageStateConst.PUB);
                sendPubRecMessage(ctx, mqttPublishMessage);
                break;
        }
    }

    public static void replyPubRelMessage(ChannelHandlerContext ctx, MqttMessage mqttMessage){
        MqttPublishMessage mqttPublishMessage = (MqttPublishMessage)mqttMessage;
//        updateMessage(mqttPublishMessage.variableHeader().messageId(), MqttMessageStateConst.COMP);
        sendPubCompMessage(ctx, mqttPublishMessage);
        pushPublishTopic(ctx,  mqttPublishMessage);
    }

    public static void replyDisConnectMessage(ChannelHandlerContext ctx){
        forceClose(ctx);
    }

    public static void sendPubAckMessage(ChannelHandlerContext ctx, MqttPublishMessage mqttPublishMessage){
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK,false, MqttQoS.AT_MOST_ONCE,false,0x02);
        int messageId = mqttPublishMessage.variableHeader().messageId();
        MqttMessageIdVariableHeader from = MqttMessageIdVariableHeader.from(messageId);
        MqttPubAckMessage mqttPubAckMessage = new MqttPubAckMessage(mqttFixedHeader, from);
        writeAndFlush(ctx, mqttPubAckMessage);
    }

    public static void sendPubRecMessage(ChannelHandlerContext ctx, MqttPublishMessage mqttPublishMessage){
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC,false, MqttQoS.AT_LEAST_ONCE,false,0x02);
        int messageId = mqttPublishMessage.variableHeader().messageId();
        MqttMessageIdVariableHeader from = MqttMessageIdVariableHeader.from(messageId);
        MqttPubAckMessage mqttPubRecMessage = new MqttPubAckMessage(mqttFixedHeader, from);
        writeAndFlush(ctx, mqttPubRecMessage);
    }

    public static void sendPubCompMessage(ChannelHandlerContext ctx, MqttMessage mqttMessage){
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBCOMP,false, MqttQoS.AT_MOST_ONCE,false,0x02);
        int messageId = ((MqttMessageIdVariableHeader)mqttMessage.variableHeader()).messageId();
        MqttMessageIdVariableHeader from = MqttMessageIdVariableHeader.from(messageId);
        MqttPubAckMessage mqttPubCompMessage = new MqttPubAckMessage(mqttFixedHeader, from);
        writeAndFlush(ctx, mqttPubCompMessage);
    }

    public static void sendDisConnectMessage(ChannelHandlerContext ctx, MqttConnectMessage mqttConnectMessage){
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.DISCONNECT, mqttConnectMessage.fixedHeader().isDup(), MqttQoS.AT_MOST_ONCE, mqttConnectMessage.fixedHeader().isRetain(), 0x02);
        MqttMessage mqttMessage = new MqttMessage(mqttFixedHeader);
        writeAndFlush(ctx, mqttMessage);
    }

    public static void replySubscribeMessage(ChannelHandlerContext ctx, MqttSubscribeMessage mqttSubscribeMessage){
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.SUBACK, mqttSubscribeMessage.fixedHeader().isDup(), MqttQoS.AT_MOST_ONCE, mqttSubscribeMessage.fixedHeader().isRetain(), 0);
        MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(mqttSubscribeMessage.variableHeader().messageId());
        int num = mqttSubscribeMessage.payload().topicSubscriptions().size();
        List<Integer> grantedQoSLevels = new ArrayList<>(num);
        for (int i = 0; i < num; i++) {
            grantedQoSLevels.add(mqttSubscribeMessage.payload().topicSubscriptions().get(i).qualityOfService().value());
            //
            String topicName = mqttSubscribeMessage.payload().topicSubscriptions().get(i).topicName();
            Set<MqttTopic> mqttTopics = topics.get(topicName);
            if( mqttTopics == null ){
                mqttTopics = new HashSet<>();
            }
            MqttTopic mqttTopic = new MqttTopic();
            mqttTopic.setTopicName(topicName);
            mqttTopic.setMqttQoS(mqttSubscribeMessage.payload().topicSubscriptions().get(i).qualityOfService());
            mqttTopic.setCtx(ctx);
            mqttTopics.add(mqttTopic);
            topics.put(topicName, mqttTopics);
        }
        MqttSubAckPayload payload = new MqttSubAckPayload(grantedQoSLevels);
        MqttSubAckMessage mqttSubAckMessage = new MqttSubAckMessage(mqttFixedHeader, variableHeader, payload);
        writeAndFlush(ctx, mqttSubAckMessage);
    }

    public static void sendWillMessage(ChannelHandlerContext ctx){
        String deviceId = ctx.channel().attr(_deviceId).get();
        MqttChannel mqttChannel = channels.get(deviceId);
        if( mqttChannel != null ){
            MqttWill mqttWill = mqttChannel.getMqttWill();
            if( mqttWill != null ){
                String willTopic = mqttWill.getWillTopic();
                byte[] willMessage = mqttWill.getWillMessage();
                MqttQoS mqttQoS = mqttWill.getMqttQoS();
                Set<MqttTopic> mqttTopics = topics.get(willTopic);
                for (MqttTopic mqttTopic : mqttTopics){
                    switch (mqttWill.getMqttQoS()){
                        case AT_MOST_ONCE:
                            sendTopicMessage(mqttTopic.getCtx(), mqttWill.getWillTopic(), mqttWill.getWillMessage(), com.netty.server.utils.MessageIdUtil.messageId(), mqttWill.getMqttQoS());
                            break;
                        case AT_LEAST_ONCE:
                            // 先不实现
                            break;
                        case EXACTLY_ONCE:
                            // 先不实现
                            break;
                    }
                }
            }else{
                System.out.println("Info: MqttWill 未设置");
            }
        }else{
            System.out.println("Error: MqttChannel 不存在");
        }

//        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.DISCONNECT, mqttConnectMessage.fixedHeader().isDup(), MqttQoS.AT_MOST_ONCE, mqttConnectMessage.fixedHeader().isRetain(), 0x02);
//        MqttMessage mqttMessage = new MqttMessage(mqttFixedHeader);
//        writeAndFlush(ctx, mqttMessage);
    }

    public static Boolean checkLogin(ChannelHandlerContext ctx){
        if( !ctx.channel().hasAttr(_login) ) {
            return false;
        }
        return ctx.channel().attr(_login).get();
    }

    public static void checkAlive(){
        for (String deviceId: channels.keySet()) {
            MqttChannel mqttChannel = channels.get(deviceId);
            if( checkOvertime(mqttChannel.getActiveTime(), mqttChannel.getKeepAlive()) ){
                // 在1.5个心跳周期内没有收到心跳包，则断开与客户端的链接
                System.out.println("Info：客户端（"+mqttChannel.getDeviceId()+"）心跳超时，强制断开链接");
                forceClose(mqttChannel.getCtx());
            }
        }
    }

//    private static void saveMessage(ChannelHandlerContext ctx, int messageId, String topicName, byte[] payload, MqttQoS mqttQoS, int state){
//        MqttSendMessage mqttSendMessage = new MqttSendMessage();
//        mqttSendMessage.setMessageId(messageId);
//        mqttSendMessage.setTopicName(topicName);
//        mqttSendMessage.setPayload(payload);
//        mqttSendMessage.setMqttQoS(mqttQoS);
//        mqttSendMessage.setState(state);
//        mqttSendMessage.setCtx(ctx);
//        mqttSendMessage.setSendTime(new Date().getTime());
//        messages.put(messageId, mqttSendMessage);
//    }
//
//    private static void updateMessage(int messageId, int state){
//        MqttSendMessage mqttSendMessage = messages.get(messageId);
//        if( mqttSendMessage != null ){
//            mqttSendMessage.setState(state);
//            mqttSendMessage.setSendTime(new Date().getTime());
//        }else{
//            System.out.println("Error：找不到对应的message");
//        }
//    }

    private static boolean checkOvertime(long activeTime, long keepAlive) {
        System.out.println(System.currentTimeMillis()-activeTime);
        return System.currentTimeMillis()-activeTime>=keepAlive*1.5*1000;
    }

    public static void forceClose(ChannelHandlerContext ctx){
        Channel channel = ctx.channel();
        String deviceId = channel.attr(_deviceId).get();
        MqttChannel mqttChannel = channels.get(deviceId);
        mqttChannel.getCtx().channel().close();
        channels.remove(deviceId);
    }

    private static void pushPublishTopic(ChannelHandlerContext ctx, MqttPublishMessage mqttPublishMessage){
        String topicName = mqttPublishMessage.variableHeader().topicName();

        ByteBuf byteBuf = Unpooled.copiedBuffer(mqttPublishMessage.payload());
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        System.out.println(new String(bytes));

        Set<MqttTopic> mqttTopics = topics.get(topicName);
        for (MqttTopic mqttTopic : mqttTopics){
            if( mqttTopic.getCtx().channel().isActive() && mqttTopic.getCtx().channel().isWritable() ){
                sendTopicMessage(mqttTopic.getCtx(), topicName, bytes, com.netty.server.utils.MessageIdUtil.messageId(), mqttTopic.getMqttQoS());
            }
        }
    }

    private static void sendTopicMessage(ChannelHandlerContext ctx, String topicName, byte[] bytes, int messageId, MqttQoS mqttQoS){
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH,false, mqttQoS,false,0);
        MqttPublishVariableHeader mqttPublishVariableHeader = new MqttPublishVariableHeader(topicName, messageId);
        MqttPublishMessage mqttPublishMessage = new MqttPublishMessage(mqttFixedHeader,mqttPublishVariableHeader,Unpooled.wrappedBuffer(bytes));
        writeAndFlush(ctx, mqttPublishMessage);
    }

    private static void writeAndFlush(ChannelHandlerContext ctx, MqttMessage mqttMessage){
        ctx.writeAndFlush(mqttMessage).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                try {
                    if (future.isSuccess()) {
                        System.out.println("回写成功：" + mqttMessage);
                    } else {
                        System.out.println("回写失败：" + mqttMessage);
                    }
                }catch (IllegalReferenceCountException e){
                    // Do nothing
                }

            }
        });
    }
}
