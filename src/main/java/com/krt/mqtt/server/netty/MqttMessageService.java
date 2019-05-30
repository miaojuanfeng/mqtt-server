package com.krt.mqtt.server.netty;

import com.krt.mqtt.server.beans.MqttChannel;
import com.krt.mqtt.server.beans.MqttSendMessage;
import com.krt.mqtt.server.beans.MqttTopic;
import com.krt.mqtt.server.beans.MqttWill;
import com.krt.mqtt.server.constant.MqttMessageStateConst;
import com.krt.mqtt.server.service.UserService;
import com.krt.mqtt.server.utils.MessageIdUtil;
import com.krt.mqtt.server.utils.MqttUtil;
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
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class MqttMessageService {

    @Autowired
    private UserService userService;

    public static final AttributeKey<Boolean> _login = AttributeKey.valueOf("login");

    public static final AttributeKey<String> _deviceId = AttributeKey.valueOf("deviceId");

    /**
     * 已连接到服务器端的通道
     */
    private static ConcurrentHashMap<String, MqttChannel> channels = new ConcurrentHashMap<>();

    /**
     * 所有主题列表，以及所有订阅该主题的通道
     */
    private static ConcurrentHashMap<String, ConcurrentHashMap<String, MqttTopic>> topics = new ConcurrentHashMap<>();

    /**
     * 未完成的回复报文
     */
    private static ConcurrentHashMap<Integer, MqttSendMessage> replyMessages = new ConcurrentHashMap<>();

    /**
     * 未完成的发送报文
     */
    private static ConcurrentHashMap<Integer, MqttSendMessage> sendMessages = new ConcurrentHashMap<>();

    public void replyConnectMessage(ChannelHandlerContext ctx, MqttConnectMessage mqttConnectMessage){
        /**
         * Mqtt协议规定，相同Client ID客户端已连接到服务器，
         * 先前客户端必须断开连接后，服务器才能完成新的客户端CONNECT连接
         */
        String deviceId = mqttConnectMessage.payload().clientIdentifier();
        /**
         * 检查是否有重复连接的客户端
         */
        MqttChannel existChannel = channels.get(deviceId);
        if( existChannel != null ){
            log.info("客户端（"+deviceId+"）已连接上服务器，断开该客户端之前的连接");
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
        channels.put(deviceId, mqttChannel);
        /**
         * 设置客户端通道属性
         */
        Channel channel = ctx.channel();
        channel.attr(_login).set(true);
        channel.attr(_deviceId).set(deviceId);
        /**
         * 回写连接确认报文
         */
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.CONNACK, mqttConnectMessage.fixedHeader().isDup(), MqttQoS.AT_MOST_ONCE, mqttConnectMessage.fixedHeader().isRetain(), 0x02);
        MqttConnAckVariableHeader mqttConnAckVariableHeader = new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_ACCEPTED, false);
        MqttConnAckMessage connAck = new MqttConnAckMessage(mqttFixedHeader, mqttConnAckVariableHeader);
        writeAndFlush(ctx, connAck);
    }

    public void replyPingReqMessage(ChannelHandlerContext ctx){
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttMessage pingResp = new MqttMessage(mqttFixedHeader);
        writeAndFlush(ctx, pingResp);
    }

    public void updateActiveTime(ChannelHandlerContext ctx){
        String deviceId = ctx.channel().attr(_deviceId).get();
        MqttChannel mqttChannel = channels.get(deviceId);
        mqttChannel.setActiveTime(new Date().getTime());
    }

    public void replyPublishMessage(ChannelHandlerContext ctx, MqttPublishMessage mqttPublishMessage){
//        ByteBuf byteBuffer = mqttPublishMessage.payload();
//        byte[] bytes = new byte[byteBuffer.readableBytes()];
//        byteBuffer.readBytes(bytes);
//        String content = new String(bytes);
//        System.out.println("publish name: "+mqttPublishMessage.variableHeader().topicName());
//        System.out.println("publish content: "+content);

        String topicName = mqttPublishMessage.variableHeader().topicName();
        byte[] topicMessage = MqttUtil.readBytes(mqttPublishMessage.payload());
        /**
         * 根据客户端发来的报文类型来决定回复客户端的报文类型
         */
        switch (mqttPublishMessage.fixedHeader().qosLevel()){
            case AT_MOST_ONCE:
                pushPublishTopic(ctx, topicName, topicMessage);
                break;
            case AT_LEAST_ONCE:
                sendPubAckMessage(ctx, mqttPublishMessage.variableHeader().messageId());
                pushPublishTopic(ctx, topicName, topicMessage);
                break;
            case EXACTLY_ONCE:
                /**
                 * 检查一下消息是否重复，是否需要idDup标识位
                 */
                if( !mqttPublishMessage.fixedHeader().isDup() ||
                        !checkExistReplyMessage(mqttPublishMessage.variableHeader().messageId()) ) {
                    saveReplyMessage(ctx,
                            mqttPublishMessage.variableHeader().messageId(),
                            mqttPublishMessage.variableHeader().topicName(),
                            topicMessage,
                            MqttMessageStateConst.REC);
                }
                sendPubRecMessage(ctx, mqttPublishMessage.variableHeader().messageId());
                break;
        }
    }

    public void replyPubAckMessage(ChannelHandlerContext ctx, MqttPubAckMessage mqttPubAckMessage){
        int messageId = mqttPubAckMessage.variableHeader().messageId();
        MqttSendMessage mqttSendMessage = sendMessages.get(messageId);
        if( mqttSendMessage != null ){
            if( mqttSendMessage.getMqttQoS() == MqttQoS.AT_LEAST_ONCE && mqttSendMessage.getState() == MqttMessageStateConst.PUB ){
                sendMessages.remove(messageId);
            }else{
                log.error("未完成发布报文（"+messageId+"）状态错误（"+mqttSendMessage.getMqttQoS()+", "+mqttSendMessage.getState()+"）");
            }
        }else{
            log.error("未完成发布报文（"+messageId+"）不存在");
        }
    }

    public void replyPubRecMessage(ChannelHandlerContext ctx, MqttMessage mqttMessage){
        int messageId = ((MqttMessageIdVariableHeader)mqttMessage.variableHeader()).messageId();
        updateSendMessage(messageId, MqttMessageStateConst.REL);
        sendPubRelMessage(ctx, messageId);
    }

    public void replyPubRelMessage(ChannelHandlerContext ctx, MqttMessage mqttMessage){
        int messageId = ((MqttMessageIdVariableHeader)mqttMessage.variableHeader()).messageId();
        sendPubCompMessage(ctx, mqttMessage);
        completeReplyMessage(messageId);
    }

    public void replyPubCompMessage(ChannelHandlerContext ctx, MqttMessage mqttMessage){
        int messageId = ((MqttMessageIdVariableHeader)mqttMessage.variableHeader()).messageId();
        completeSendMessage(messageId);
    }

    public void replyDisConnectMessage(ChannelHandlerContext ctx){
        forceClose(ctx);
    }

    public void sendPubAckMessage(ChannelHandlerContext ctx, int messageId){
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK,false, MqttQoS.AT_MOST_ONCE,false,0x02);
        MqttMessageIdVariableHeader from = MqttMessageIdVariableHeader.from(messageId);
        MqttPubAckMessage mqttPubAckMessage = new MqttPubAckMessage(mqttFixedHeader, from);
        writeAndFlush(ctx, mqttPubAckMessage);
    }

    public void sendPubRecMessage(ChannelHandlerContext ctx, int messageId){
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC,false, MqttQoS.AT_LEAST_ONCE,false,0x02);
        MqttMessageIdVariableHeader from = MqttMessageIdVariableHeader.from(messageId);
        MqttPubAckMessage mqttPubRecMessage = new MqttPubAckMessage(mqttFixedHeader, from);
        writeAndFlush(ctx, mqttPubRecMessage);
    }

    public void sendPubRelMessage(ChannelHandlerContext ctx, int messageId){
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBREL,false, MqttQoS.AT_LEAST_ONCE,false,0x02);
        MqttMessageIdVariableHeader from = MqttMessageIdVariableHeader.from(messageId);
        MqttPubAckMessage mqttPubRecMessage = new MqttPubAckMessage(mqttFixedHeader, from);
        writeAndFlush(ctx, mqttPubRecMessage);
    }

    public void sendPubCompMessage(ChannelHandlerContext ctx, MqttMessage mqttMessage){
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBCOMP,false, MqttQoS.AT_MOST_ONCE,false,0x02);
        int messageId = ((MqttMessageIdVariableHeader)mqttMessage.variableHeader()).messageId();
        MqttMessageIdVariableHeader from = MqttMessageIdVariableHeader.from(messageId);
        MqttPubAckMessage mqttPubCompMessage = new MqttPubAckMessage(mqttFixedHeader, from);
        writeAndFlush(ctx, mqttPubCompMessage);
    }

    public void sendDisConnectMessage(ChannelHandlerContext ctx, MqttConnectMessage mqttConnectMessage){
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.DISCONNECT, mqttConnectMessage.fixedHeader().isDup(), MqttQoS.AT_MOST_ONCE, mqttConnectMessage.fixedHeader().isRetain(), 0x02);
        MqttMessage mqttMessage = new MqttMessage(mqttFixedHeader);
        writeAndFlush(ctx, mqttMessage);
    }

    public void replySubscribeMessage(ChannelHandlerContext ctx, MqttSubscribeMessage mqttSubscribeMessage){
        String deviceId = ctx.channel().attr(_deviceId).get();
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.SUBACK, mqttSubscribeMessage.fixedHeader().isDup(), MqttQoS.AT_MOST_ONCE, mqttSubscribeMessage.fixedHeader().isRetain(), 0);
        MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(mqttSubscribeMessage.variableHeader().messageId());
        int num = mqttSubscribeMessage.payload().topicSubscriptions().size();
        List<Integer> grantedQoSLevels = new ArrayList<>(num);
        for (int i = 0; i < num; i++) {
            grantedQoSLevels.add(mqttSubscribeMessage.payload().topicSubscriptions().get(i).qualityOfService().value());
            //
            String topicName = mqttSubscribeMessage.payload().topicSubscriptions().get(i).topicName();
            ConcurrentHashMap<String, MqttTopic> mqttTopics = topics.get(topicName);
            if( mqttTopics == null ){
                mqttTopics = new ConcurrentHashMap<>();
            }
            MqttTopic mqttTopic = new MqttTopic();
            mqttTopic.setTopicName(topicName);
            mqttTopic.setMqttQoS(mqttSubscribeMessage.payload().topicSubscriptions().get(i).qualityOfService());
            mqttTopic.setCtx(ctx);
            mqttTopics.put(deviceId, mqttTopic);
            topics.put(topicName, mqttTopics);
        }
        MqttSubAckPayload payload = new MqttSubAckPayload(grantedQoSLevels);
        MqttSubAckMessage mqttSubAckMessage = new MqttSubAckMessage(mqttFixedHeader, variableHeader, payload);
        writeAndFlush(ctx, mqttSubAckMessage);
    }

    public void replyUnsubscribeMessage(ChannelHandlerContext ctx, MqttUnsubscribeMessage mqttUnsubscribeMessage){
        String deviceId = ctx.channel().attr(_deviceId).get();
        int num = mqttUnsubscribeMessage.payload().topics().size();
        for (int i = 0; i < num; i++) {
            String topicName = mqttUnsubscribeMessage.payload().topics().get(i);
            ConcurrentHashMap<String, MqttTopic> mqttTopics = topics.get(topicName);
            if( mqttTopics != null ){
                mqttTopics.remove(deviceId);
            }
            if( mqttTopics.size() == 0 ){
                topics.remove(topicName);
            }
        }

        int messageId = mqttUnsubscribeMessage.variableHeader().messageId();
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.UNSUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0x02);
        MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(messageId);
        MqttUnsubAckMessage mqttUnsubAckMessage = new MqttUnsubAckMessage(mqttFixedHeader, variableHeader);
        writeAndFlush(ctx, mqttUnsubAckMessage);
    }

    public void sendWillMessage(ChannelHandlerContext ctx){
        String deviceId = ctx.channel().attr(_deviceId).get();
        MqttChannel mqttChannel = channels.get(deviceId);
        if( mqttChannel != null ){
            MqttWill mqttWill = mqttChannel.getMqttWill();
            if( mqttWill != null ){
                String willTopic = mqttWill.getWillTopic();
                byte[] willMessage = mqttWill.getWillMessage();
                MqttQoS mqttQoS = mqttWill.getMqttQoS();
                ConcurrentHashMap<String, MqttTopic> mqttTopics = topics.get(willTopic);
                for (String key : mqttTopics.keySet()){
                    MqttTopic mqttTopic = mqttTopics.get(key);
                    switch (mqttWill.getMqttQoS()){
                        case AT_MOST_ONCE:
                            break;
                        /**
                         * 1和2服务质量都需要将报文保存下来
                         */
                        case AT_LEAST_ONCE:
                        case EXACTLY_ONCE:
                            saveSendMessage(ctx,
                                    MessageIdUtil.messageId(),
                                    mqttWill.getWillTopic(),
                                    mqttWill.getWillMessage(),
                                    mqttWill.getMqttQoS(),
                                    MqttMessageStateConst.PUB);
                            break;
                    }
                    sendTopicMessage(mqttTopic.getCtx(), mqttWill.getWillTopic(), mqttWill.getWillMessage(), MessageIdUtil.messageId(), mqttWill.getMqttQoS());
                }
            }else{
                log.info("客户端（"+deviceId+"）遗愿未设置");
            }
        }else{
            log.error("客户端（"+deviceId+"）通道不存在");
        }
    }

    public Boolean checkLogin(ChannelHandlerContext ctx){
        if( !ctx.channel().hasAttr(_login) ) {
            return false;
        }
        return ctx.channel().attr(_login).get();
    }

    public void checkAlive(){
        for (String deviceId: channels.keySet()) {
            MqttChannel mqttChannel = channels.get(deviceId);
            if( checkOvertime(mqttChannel.getActiveTime(), mqttChannel.getKeepAlive()) ){
                /**
                 * 在1.5个心跳周期内没有收到心跳包，则断开与客户端的链接
                  */
                log.info("客户端（"+mqttChannel.getDeviceId()+"）心跳超时，强制断开链接");
                forceClose(mqttChannel.getCtx());
            }
        }
    }

    private void saveReplyMessage(ChannelHandlerContext ctx, int messageId, String topicName, byte[] payload, int state){
        MqttSendMessage mqttSendMessage = new MqttSendMessage();
        mqttSendMessage.setMessageId(messageId);
        mqttSendMessage.setTopicName(topicName);
        mqttSendMessage.setPayload(payload);
        mqttSendMessage.setState(state);
        mqttSendMessage.setCtx(ctx);
        mqttSendMessage.setSendTime(new Date().getTime());
        mqttSendMessage.setResendCount(1);
        replyMessages.put(messageId, mqttSendMessage);
    }

    private void saveSendMessage(ChannelHandlerContext ctx, int messageId, String topicName, byte[] payload, MqttQoS mqttQoS, int state){
        MqttSendMessage mqttSendMessage = new MqttSendMessage();
        mqttSendMessage.setMessageId(messageId);
        mqttSendMessage.setTopicName(topicName);
        mqttSendMessage.setPayload(payload);
        mqttSendMessage.setState(state);
        mqttSendMessage.setMqttQoS(mqttQoS);
        mqttSendMessage.setCtx(ctx);
        mqttSendMessage.setSendTime(new Date().getTime());
        mqttSendMessage.setResendCount(0);
        sendMessages.put(messageId, mqttSendMessage);
    }

    public void updateReplyMessage(int messageId){
        MqttSendMessage mqttSendMessage = replyMessages.get(messageId);
        if( mqttSendMessage != null ){
            mqttSendMessage.setSendTime(new Date().getTime());
            mqttSendMessage.setResendCount(mqttSendMessage.getResendCount()+1);
        }else{
            log.error("未完成回复报文（"+messageId+"）不存在");
        }
    }

    public void updateSendMessage(int messageId, int state){
        MqttSendMessage mqttSendMessage = sendMessages.get(messageId);
        if( mqttSendMessage != null ){
            mqttSendMessage.setSendTime(new Date().getTime());
            mqttSendMessage.setState(state);
            mqttSendMessage.setResendCount(1);
        }else{
            log.error("未完成回复报文（"+messageId+"）不存在");
        }
    }

    public void updateSendMessage(int messageId){
        MqttSendMessage mqttSendMessage = sendMessages.get(messageId);
        if( mqttSendMessage != null ){
            mqttSendMessage.setSendTime(new Date().getTime());
            mqttSendMessage.setResendCount(mqttSendMessage.getResendCount()+1);
        }else{
            log.error("未完成回复报文（"+messageId+"）不存在");
        }
    }

    public void completeReplyMessage(int messageId){
        MqttSendMessage mqttSendMessage = replyMessages.get(messageId);
        if( mqttSendMessage != null && mqttSendMessage.getState() == MqttMessageStateConst.REC ){
            pushPublishTopic(mqttSendMessage.getCtx(), mqttSendMessage.getTopicName(), mqttSendMessage.getPayload());
            replyMessages.remove(messageId);
        }else{
            log.error("未完成回复报文（"+messageId+"）不存在");
        }
    }

    public void completeSendMessage(int messageId){
        MqttSendMessage mqttSendMessage = sendMessages.get(messageId);
        if( mqttSendMessage != null && mqttSendMessage.getState() == MqttMessageStateConst.REL ){
            sendMessages.remove(messageId);
        }else{
            log.error("未完成回复报文（"+messageId+"）不存在");
        }
    }

    public void resendReplyMessage(){
        for (Integer messageId: replyMessages.keySet()){
            MqttSendMessage mqttSendMessage = replyMessages.get(messageId);
            if( mqttSendMessage.getState() == MqttMessageStateConst.REC ){
                if( mqttSendMessage.getCtx().channel().isActive() && mqttSendMessage.getCtx().channel().isWritable() ){
                    if( checkReplyTime(mqttSendMessage.getSendTime(), mqttSendMessage.getResendCount()) ) {
                        sendPubRecMessage(mqttSendMessage.getCtx(), messageId);
                        updateReplyMessage(messageId);
                    }
                }else{
                    /**
                     * 通道已关闭，删除消息
                     */
                    replyMessages.remove(messageId);
                }
            }
        }
    }

    public void resendSendMessage(){
        for (Integer messageId: sendMessages.keySet()){
            MqttSendMessage mqttSendMessage = sendMessages.get(messageId);
            if( mqttSendMessage.getCtx().channel().isActive() && mqttSendMessage.getCtx().channel().isWritable() ){
                if( mqttSendMessage.getState() == MqttMessageStateConst.PUB ) {
                    if (checkReplyTime(mqttSendMessage.getSendTime(), mqttSendMessage.getResendCount())) {
                        sendTopicMessage(mqttSendMessage.getCtx(), mqttSendMessage.getTopicName(), mqttSendMessage.getPayload(), mqttSendMessage.getMessageId(), mqttSendMessage.getMqttQoS());
                        updateSendMessage(messageId);
                    }
                }else if(  mqttSendMessage.getState() == MqttMessageStateConst.REL ){
                    sendPubRelMessage(mqttSendMessage.getCtx(), messageId);
                }else{
                    log.error("未完成发布报文（"+messageId+"）状态错误（"+mqttSendMessage.getMqttQoS()+", "+mqttSendMessage.getState()+"）");
                }
            }else{
                /**
                 * 通道已关闭，删除消息
                 */
                replyMessages.remove(messageId);
            }
        }
    }

    private boolean checkExistReplyMessage(int messageId){
        MqttSendMessage mqttSendMessage = replyMessages.get(messageId);
        if( mqttSendMessage != null && mqttSendMessage.getState() == MqttMessageStateConst.REC ){
            return true;
        }else{
            return false;
        }
    }

    private boolean checkOvertime(long activeTime, long keepAlive) {
        return System.currentTimeMillis()-activeTime>=keepAlive*1.5*1000;
    }

    private boolean checkReplyTime(long sendTime, long interval) {
        return System.currentTimeMillis()-sendTime>=interval*2*1000;
    }

    public void forceClose(ChannelHandlerContext ctx){
        Channel channel = ctx.channel();
        String deviceId = channel.attr(_deviceId).get();
        MqttChannel mqttChannel = channels.get(deviceId);
        if( mqttChannel != null ) {
            mqttChannel.getCtx().channel().close();
            channels.remove(deviceId);
        }
    }

//    private void pushPublishTopic(ChannelHandlerContext ctx, MqttPublishMessage mqttPublishMessage){
//        String topicName = mqttPublishMessage.variableHeader().topicName();
//        byte[] topicMessage = MqttUtil.readBytes(mqttPublishMessage.payload());
//        System.out.println("topicMessage: "+new String(topicMessage));
//        pushPublishTopic(ctx, topicName, topicMessage);
//    }

    private void pushPublishTopic(ChannelHandlerContext ctx, String topicName, byte[] payload){
        /**
         * 遍历所有订阅了该主题的客户端
         */
        ConcurrentHashMap<String, MqttTopic> mqttTopics = topics.get(topicName);
        if( mqttTopics != null ) {
            for (String key : mqttTopics.keySet()) {
                MqttTopic mqttTopic = mqttTopics.get(key);
                if (mqttTopic.getCtx().channel().isActive() && mqttTopic.getCtx().channel().isWritable()) {
                    int messageId = MessageIdUtil.messageId();
                    if( mqttTopic.getMqttQoS().value() > MqttQoS.AT_MOST_ONCE.value() ) {
                        saveSendMessage(ctx, messageId, topicName, payload, mqttTopic.getMqttQoS(), MqttMessageStateConst.PUB);
                    }
                    sendTopicMessage(mqttTopic.getCtx(), topicName, payload, messageId, mqttTopic.getMqttQoS());
                }
            }
        }
    }

    private void sendTopicMessage(ChannelHandlerContext ctx, String topicName, byte[] bytes, int messageId, MqttQoS mqttQoS){
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH,false, mqttQoS,false,0);
        MqttPublishVariableHeader mqttPublishVariableHeader = new MqttPublishVariableHeader(topicName, messageId);
        MqttPublishMessage mqttPublishMessage = new MqttPublishMessage(mqttFixedHeader,mqttPublishVariableHeader,Unpooled.wrappedBuffer(bytes));
        writeAndFlush(ctx, mqttPublishMessage);
    }

    private void writeAndFlush(ChannelHandlerContext ctx, MqttMessage mqttMessage){
        ctx.writeAndFlush(mqttMessage).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                try {
                    if (future.isSuccess()) {
                        log.info("服务器（" + ctx.channel().attr(_deviceId).get() + "）回写报文：" + mqttMessage);
                    } else {
                        log.error("服务器（" + ctx.channel().attr(_deviceId).get() + "）回写失败：" + mqttMessage);
                    }
                }catch (IllegalReferenceCountException e){
                    // Do nothing
                }
            }
        });
    }
}
