package com.krt.mqtt.server.netty;

import com.krt.mqtt.server.beans.MqttChannel;
import com.krt.mqtt.server.beans.MqttSendMessage;
import com.krt.mqtt.server.beans.MqttTopic;
import com.krt.mqtt.server.beans.MqttWill;
import com.krt.mqtt.server.constant.MqttMessageStateConst;
import com.krt.mqtt.server.entity.Message;
import com.krt.mqtt.server.entity.Subscribe;
import com.krt.mqtt.server.service.DeviceService;
import com.krt.mqtt.server.service.MessageService;
import com.krt.mqtt.server.service.SubscribeService;
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

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class MqttMessageService {

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private SubscribeService subscribeService;

    @Autowired
    private MqttChannelApi mqttChannelApi;

    @Autowired
    private MqttMessageApi mqttMessageApi;

    private static final int maxResendCount = 10;

    /**
     * 所有主题列表，以及所有订阅该主题的通道
     */
    private static ConcurrentHashMap<String, ConcurrentHashMap<String, MqttTopic>> topics = new ConcurrentHashMap<>();

    public void replyConnectMessage(ChannelHandlerContext ctx, MqttConnectMessage mqttConnectMessage){
        /**
         * Mqtt协议规定，相同Client ID客户端已连接到服务器，
         * 先前客户端必须断开连接后，服务器才能完成新的客户端CONNECT连接
         */
        String deviceId = mqttConnectMessage.payload().clientIdentifier();
        /**
         * 检查是否有重复连接的客户端
         */
        MqttChannel existChannel = mqttChannelApi.getChannel(deviceId);
        if( existChannel != null ){
            log.info("客户端（"+deviceId+"）已连接上服务器，断开该客户端之前的连接");
            mqttMessageApi.DISCONNECT(existChannel.getCtx());
            mqttChannelApi.closeChannel(existChannel.getCtx());
        }
        /**
         * 创建一个新的客户端实例
         */
        MqttChannel mqttChannel = new MqttChannel();
        mqttChannel.setDeviceId(deviceId);
        mqttChannel.setCtx(ctx);
        mqttChannel.setKeepAlive(mqttConnectMessage.variableHeader().keepAliveTimeSeconds());
        mqttChannel.setActiveTime(new Date().getTime());
        mqttChannel.setReplyMessages(new ConcurrentHashMap<>());
        mqttChannel.setSendMessages(new ConcurrentHashMap<>());
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
        mqttChannelApi.setChannel(deviceId, mqttChannel);
        /**
         * 持久化客户端信息
         */
        InetSocketAddress remoteAddress = (InetSocketAddress)ctx.channel().remoteAddress();
        Integer dbId = deviceService.doLogin(deviceId, remoteAddress.getAddress().getHostAddress(), remoteAddress.getPort());
        /**
         * 设置客户端通道属性
         */
        mqttChannelApi.setChannelAttr(ctx, deviceId, dbId);
        /**
         * 回写连接确认报文
         */
        mqttMessageApi.CONNACK(ctx, mqttConnectMessage.fixedHeader().isDup());
    }

    public void replyPublishMessage(ChannelHandlerContext ctx, MqttPublishMessage mqttPublishMessage){
        int messageId = mqttPublishMessage.variableHeader().packetId();
        String topicName = mqttPublishMessage.variableHeader().topicName();
        byte[] topicMessage = MqttUtil.readBytes(mqttPublishMessage.payload());
        /**
         * 持久化发布报文
         */
        messageService.insert(mqttChannelApi.getChannelDbId(ctx), messageId, topicName, topicMessage);
        /**
         * 根据客户端发来的报文类型来决定回复客户端的报文类型
         */
        switch (mqttPublishMessage.fixedHeader().qosLevel()){
            case AT_MOST_ONCE:
                pushPublishTopic(ctx, topicName, topicMessage);
                break;
            case AT_LEAST_ONCE:
                mqttMessageApi.PUBACK(ctx, messageId);
                pushPublishTopic(ctx, topicName, topicMessage);
                break;
            case EXACTLY_ONCE:
                /**
                 * 检查一下消息是否重复，是否需要idDup标识位
                 */
                if( !mqttPublishMessage.fixedHeader().isDup() ||
                        !checkExistReplyMessage(ctx, messageId) ) {
                    saveReplyMessage(ctx,
                            messageId,
                            topicName,
                            topicMessage,
                            MqttMessageStateConst.REC);
                }
                mqttMessageApi.PUBREC(ctx, messageId, false);
                break;
        }
    }

    public void replyPubAckMessage(ChannelHandlerContext ctx, MqttPubAckMessage mqttPubAckMessage){
        int messageId = mqttPubAckMessage.variableHeader().messageId();
        ConcurrentHashMap<Integer, MqttSendMessage> sendMessages = mqttChannelApi.getSendMessages(ctx);
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
        updateSendMessage(ctx, messageId, MqttMessageStateConst.REL);
        mqttMessageApi.PUBREL(ctx, messageId);
    }

    public void replyPubRelMessage(ChannelHandlerContext ctx, MqttMessage mqttMessage){
        int messageId = ((MqttMessageIdVariableHeader)mqttMessage.variableHeader()).messageId();
        completeReplyMessage(ctx, messageId);
        mqttMessageApi.PUBCOMP(ctx, messageId);
    }

    public void replyPubCompMessage(ChannelHandlerContext ctx, MqttMessage mqttMessage){
        int messageId = ((MqttMessageIdVariableHeader)mqttMessage.variableHeader()).messageId();
        completeSendMessage(ctx, messageId);
    }

    public void replySubscribeMessage(ChannelHandlerContext ctx, MqttSubscribeMessage mqttSubscribeMessage){
        String deviceId = mqttChannelApi.getChannelDeviceId(ctx);
        Integer dbId = mqttChannelApi.getChannelDbId(ctx);
        int num = mqttSubscribeMessage.payload().topicSubscriptions().size();
        List<Integer> grantedQoSLevels = new ArrayList<>(num);
        List<Subscribe> subscribes = new ArrayList<>();
        Date date = new Date();
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
            /**
             * 持久化订阅主题
             */
            Subscribe subscribe = new Subscribe();
            subscribe.setDeviceId(dbId);
            subscribe.setTopicName(topicName);
            subscribe.setInsertTime(date);
            subscribe.setUpdateTime(date);
            subscribes.add(subscribe);
        }
        subscribeService.insertBatch(subscribes);
        mqttMessageApi.SUBACK(ctx, grantedQoSLevels, mqttSubscribeMessage.variableHeader().messageId(), mqttSubscribeMessage.fixedHeader().isDup());
    }

    public void replyUnsubscribeMessage(ChannelHandlerContext ctx, MqttUnsubscribeMessage mqttUnsubscribeMessage){
        String deviceId = mqttChannelApi.getChannelDeviceId(ctx);
        int num = mqttUnsubscribeMessage.payload().topics().size();
        List<String> topicNames = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            String topicName = mqttUnsubscribeMessage.payload().topics().get(i);
            ConcurrentHashMap<String, MqttTopic> mqttTopics = topics.get(topicName);
            if( mqttTopics != null ){
                mqttTopics.remove(deviceId);
            }
            if( mqttTopics.size() == 0 ){
                topics.remove(topicName);
            }
            /**
             * 持久化到数据库
             */
            topicNames.add(topicName);
        }
        subscribeService.deleteBatch(mqttChannelApi.getChannelDbId(ctx), topicNames);
        mqttMessageApi.UNSUBACK(ctx, mqttUnsubscribeMessage.variableHeader().messageId());
    }

    public void sendWillMessage(ChannelHandlerContext ctx){
        MqttChannel mqttChannel = mqttChannelApi.getChannel(ctx);
        if( mqttChannel != null ){
            MqttWill mqttWill = mqttChannel.getMqttWill();
            if( mqttWill != null ){
                String willTopic = mqttWill.getWillTopic();
                byte[] willMessage = mqttWill.getWillMessage();
                MqttQoS mqttQoS = mqttWill.getMqttQoS();
                ConcurrentHashMap<String, MqttTopic> mqttTopics = topics.get(willTopic);
                for (String key : mqttTopics.keySet()){
                    int messageId = MessageIdUtil.messageId();
                    MqttTopic mqttTopic = mqttTopics.get(key);
                    switch (mqttWill.getMqttQoS()){
                        case AT_MOST_ONCE:
                            break;
                        /**
                         * 1和2服务质量都需要将报文保存下来
                         */
                        case AT_LEAST_ONCE:
                        case EXACTLY_ONCE:
                            saveSendMessage(mqttTopic.getCtx(),
                                    messageId,
                                    mqttWill.getWillTopic(),
                                    mqttWill.getWillMessage(),
                                    mqttWill.getMqttQoS(),
                                    MqttMessageStateConst.PUB);
                            break;
                    }
                    mqttMessageApi.PUBLISH(mqttTopic.getCtx(), mqttWill.getWillTopic(), mqttWill.getWillMessage(), messageId, mqttWill.getMqttQoS(), false);
                }
            }else{
                log.info("客户端（"+mqttChannelApi.getChannelDeviceId(ctx)+"）遗愿未设置");
            }
        }else{
            log.error("客户端（"+mqttChannelApi.getChannelDeviceId(ctx)+"）通道不存在");
        }
    }

    private void saveReplyMessage(ChannelHandlerContext ctx, int messageId, String topicName, byte[] payload, int state){
        MqttChannel mqttChannel = mqttChannelApi.getChannel(ctx);
        ConcurrentHashMap<Integer, MqttSendMessage> replyMessages = mqttChannel.getReplyMessages();
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
        ConcurrentHashMap<Integer, MqttSendMessage> sendMessages = mqttChannelApi.getSendMessages(ctx);
        MqttSendMessage mqttSendMessage = new MqttSendMessage();
        mqttSendMessage.setMessageId(messageId);
        mqttSendMessage.setTopicName(topicName);
        mqttSendMessage.setPayload(payload);
        mqttSendMessage.setState(state);
        mqttSendMessage.setMqttQoS(mqttQoS);
        mqttSendMessage.setCtx(ctx);
        mqttSendMessage.setSendTime(new Date().getTime());
        mqttSendMessage.setResendCount(1);
        sendMessages.put(messageId, mqttSendMessage);
    }

    public void updateReplyMessage(ChannelHandlerContext ctx, int messageId){
        ConcurrentHashMap<Integer, MqttSendMessage> replyMessages = mqttChannelApi.getReplyMessages(ctx);
        MqttSendMessage mqttSendMessage = replyMessages.get(messageId);
        if( mqttSendMessage != null ){
            mqttSendMessage.setSendTime(new Date().getTime());
            mqttSendMessage.setResendCount(mqttSendMessage.getResendCount()+1);
        }else{
            log.error("未完成回复报文（"+messageId+"）不存在");
        }
    }

    public void updateSendMessage(ChannelHandlerContext ctx, int messageId, Integer state){
        ConcurrentHashMap<Integer, MqttSendMessage> sendMessages = mqttChannelApi.getSendMessages(ctx);
        MqttSendMessage mqttSendMessage = sendMessages.get(messageId);
        if( mqttSendMessage != null ){
            if( state != null ){
                mqttSendMessage.setSendTime(new Date().getTime());
                mqttSendMessage.setState(state);
                mqttSendMessage.setResendCount(1);
            }else{
                mqttSendMessage.setSendTime(new Date().getTime());
                mqttSendMessage.setResendCount(mqttSendMessage.getResendCount()+1);
            }
        }else{
            log.error("未完成回复报文（"+messageId+"）不存在");
        }
    }

    public void completeReplyMessage(ChannelHandlerContext ctx, int messageId){
        ConcurrentHashMap<Integer, MqttSendMessage> replyMessages = mqttChannelApi.getReplyMessages(ctx);
        MqttSendMessage mqttSendMessage = replyMessages.get(messageId);
        if( mqttSendMessage != null && mqttSendMessage.getState() == MqttMessageStateConst.REC ){
            pushPublishTopic(mqttSendMessage.getCtx(), mqttSendMessage.getTopicName(), mqttSendMessage.getPayload());
            replyMessages.remove(messageId);
        }else{
            log.error("未完成回复报文（"+messageId+"）不存在");
        }
    }

    public void completeSendMessage(ChannelHandlerContext ctx, int messageId){
        ConcurrentHashMap<Integer, MqttSendMessage> sendMessages = mqttChannelApi.getSendMessages(ctx);
        MqttSendMessage mqttSendMessage = sendMessages.get(messageId);
        if( mqttSendMessage != null && mqttSendMessage.getState() == MqttMessageStateConst.REL ){
            sendMessages.remove(messageId);
        }else{
            log.error("未完成回复报文（"+messageId+"）不存在");
        }
    }

    public void resendReplyMessage(){
        ConcurrentHashMap<String, MqttChannel> channels = mqttChannelApi.getChannels();
        for(String deviceId: channels.keySet()) {
            MqttChannel mqttChannel = channels.get(deviceId);
            ConcurrentHashMap<Integer, MqttSendMessage> replyMessages = channels.get(deviceId).getReplyMessages();
            for (Integer messageId : replyMessages.keySet()) {
                MqttSendMessage mqttSendMessage = replyMessages.get(messageId);
                if (mqttSendMessage.getState() == MqttMessageStateConst.REC) {
                    if (mqttSendMessage.getCtx().channel().isActive() && mqttSendMessage.getCtx().channel().isWritable()) {
                        if ( mqttSendMessage.getResendCount() > maxResendCount ){
                            log.error("客户端（"+deviceId+"）未回复消息（"+messageId+"）超过最大重发次数，已忽略该消息");
                            replyMessages.remove(messageId);
                            continue;
                        }
                        if (checkResendTime(mqttSendMessage.getSendTime(), mqttSendMessage.getResendCount())) {
                            mqttMessageApi.PUBREC(mqttSendMessage.getCtx(), messageId, true);
                            updateReplyMessage(mqttChannel.getCtx(), messageId);
                        }
                    } else {
                        /**
                         * 通道已关闭，删除消息
                         */
                        replyMessages.remove(messageId);
                    }
                }
            }
        }
    }

    public void resendSendMessage(){
        ConcurrentHashMap<String, MqttChannel> channels = mqttChannelApi.getChannels();
        for(String deviceId: channels.keySet()) {
            MqttChannel mqttChannel = channels.get(deviceId);
            ConcurrentHashMap<Integer, MqttSendMessage> sendMessages = channels.get(deviceId).getSendMessages();
            for (Integer messageId : sendMessages.keySet()) {
                MqttSendMessage mqttSendMessage = sendMessages.get(messageId);
                if (mqttSendMessage.getCtx().channel().isActive() && mqttSendMessage.getCtx().channel().isWritable()) {
                    if (mqttSendMessage.getState() == MqttMessageStateConst.PUB) {
                        if ( mqttSendMessage.getResendCount() > maxResendCount ){
                            log.error("客户端（"+deviceId+"）未发送消息（"+messageId+"）超过最大重发次数，已忽略该消息");
                            sendMessages.remove(messageId);
                            continue;
                        }
                        if (checkResendTime(mqttSendMessage.getSendTime(), mqttSendMessage.getResendCount())) {
                            mqttMessageApi.PUBLISH(mqttSendMessage.getCtx(), mqttSendMessage.getTopicName(), mqttSendMessage.getPayload(), mqttSendMessage.getMessageId(), mqttSendMessage.getMqttQoS(), true);
                            updateSendMessage(mqttChannel.getCtx(), messageId, null);
                        }
                    } else if (mqttSendMessage.getState() == MqttMessageStateConst.REL) {
                        mqttMessageApi.PUBREL(mqttSendMessage.getCtx(), messageId);
                    } else {
                        log.error("未完成发布报文（" + messageId + "）状态错误（" + mqttSendMessage.getMqttQoS() + ", " + mqttSendMessage.getState() + "）");
                    }
                } else {
                    /**
                     * 通道已关闭，删除消息
                     */
                    sendMessages.remove(messageId);
                }
            }
        }
    }

    private boolean checkExistReplyMessage(ChannelHandlerContext ctx, int messageId){
        ConcurrentHashMap<Integer, MqttSendMessage> replyMessages = mqttChannelApi.getReplyMessages(ctx);
        MqttSendMessage mqttSendMessage = replyMessages.get(messageId);
        if( mqttSendMessage != null && mqttSendMessage.getState() == MqttMessageStateConst.REC ){
            return true;
        } else {
            return false;
        }
    }

    private boolean checkResendTime(long sendTime, long interval) {
        return System.currentTimeMillis()-sendTime>=interval*1000;
    }

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
                        saveSendMessage(mqttTopic.getCtx(), messageId, topicName, payload, mqttTopic.getMqttQoS(), MqttMessageStateConst.PUB);
                    }
                    mqttMessageApi.PUBLISH(mqttTopic.getCtx(), topicName, payload, messageId, mqttTopic.getMqttQoS(), false);
                }
            }
        }
    }
}
