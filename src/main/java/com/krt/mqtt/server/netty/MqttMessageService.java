package com.krt.mqtt.server.netty;


import com.krt.mqtt.server.beans.MqttChannel;
import com.krt.mqtt.server.beans.MqttSendMessage;
import com.krt.mqtt.server.beans.MqttSubject;
import com.krt.mqtt.server.beans.MqttWill;
import com.krt.mqtt.server.constant.CommonConst;
import com.krt.mqtt.server.constant.MqttMessageStateConst;
import com.krt.mqtt.server.entity.DeviceData;
import com.krt.mqtt.server.service.DeviceService;
import com.krt.mqtt.server.utils.MessageIdUtil;
import com.krt.mqtt.server.utils.MqttUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

@Slf4j
@Component
public class MqttMessageService {

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private MqttChannelApi mqttChannelApi;

    @Autowired
    private MqttMessageApi mqttMessageApi;

    @Autowired
    private MqttTopicApi mqttTopicApi;

    @Autowired
    private MqttResendApi mqttResendApi;

    @Autowired
    private NettyProcessHandler nettyProcessHandler;

    public void replyCONNECT(ChannelHandlerContext ctx, MqttConnectMessage mqttConnectMessage, Date insertTime){
        /**
         * Mqtt协议规定，相同Client ID客户端已连接到服务器，
         * 先前客户端必须断开连接后，服务器才能完成新的客户端CONNECT连接
         */
        Long deviceId = Long.valueOf(mqttConnectMessage.payload().clientIdentifier());
        /**
         * 检查是否有重复连接的客户端
         */
        MqttChannel existChannel = mqttChannelApi.getChannel(deviceId);
        if( existChannel != null ){
            log.info("客户端（"+deviceId+"）已连接上服务器，断开该客户端之前的连接");
            mqttMessageApi.DISCONNECT(existChannel.getCtx());
            mqttChannelApi.closeChannel(existChannel.getCtx(), insertTime);
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
         * 回写连接确认报文
         */
        mqttMessageApi.CONNACK(ctx, mqttConnectMessage.fixedHeader().isDup(), MqttConnectReturnCode.CONNECTION_ACCEPTED);
    }

    public void replyPUBLISH(ChannelHandlerContext ctx, MqttPublishMessage mqttPublishMessage, Date insertTime){
        int messageId = mqttPublishMessage.variableHeader().packetId();
        String topicName = mqttPublishMessage.variableHeader().topicName();
        byte[] topicMessage = MqttUtil.readBytes(mqttPublishMessage.payload());
        /**
         * 持久化发布报文
         */
        String topicContent = MqttUtil.byteToString(topicMessage);
        nettyProcessHandler.publish(ctx, topicName, topicContent, insertTime);
        /**
         * 根据客户端发来的报文类型来决定回复客户端的报文类型
         */
        switch (mqttPublishMessage.fixedHeader().qosLevel()){
            case AT_MOST_ONCE:
                broadcastPUBLISH(ctx, topicName, topicMessage);
                break;
            case AT_LEAST_ONCE:
                mqttMessageApi.PUBACK(ctx, messageId);
                broadcastPUBLISH(ctx, topicName, topicMessage);
                break;
            case EXACTLY_ONCE:
                /**
                 * 检查一下消息是否重复，是否需要idDup标识位
                 */
                if( !mqttPublishMessage.fixedHeader().isDup() || !mqttResendApi.checkExistReplyMessage(ctx, messageId) ) {
                    mqttResendApi.saveReplyMessage(ctx, messageId, topicName, topicMessage, MqttMessageStateConst.REC);
                }
                mqttMessageApi.PUBREC(ctx, messageId, false);
                break;
        }
        /**
         * 释放引用计数
         */
        mqttPublishMessage.release();
    }

    public void replyPUBACK(ChannelHandlerContext ctx, MqttPubAckMessage mqttPubAckMessage){
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

    public void replyPUBREC(ChannelHandlerContext ctx, MqttMessage mqttMessage){
        int messageId = ((MqttMessageIdVariableHeader)mqttMessage.variableHeader()).messageId();
        mqttResendApi.updateSendMessage(ctx, messageId, MqttMessageStateConst.REL);
        mqttMessageApi.PUBREL(ctx, messageId);
    }

    public void replyPUBREL(ChannelHandlerContext ctx, MqttMessage mqttMessage){
        int messageId = ((MqttMessageIdVariableHeader)mqttMessage.variableHeader()).messageId();
        mqttResendApi.completeReplyMessage(ctx, messageId);
        mqttMessageApi.PUBCOMP(ctx, messageId);
    }

    public void replyPUBCOMP(ChannelHandlerContext ctx, MqttMessage mqttMessage){
        int messageId = ((MqttMessageIdVariableHeader)mqttMessage.variableHeader()).messageId();
        mqttResendApi.completeSendMessage(ctx, messageId);
    }

    public void replySUBSCRIBE(ChannelHandlerContext ctx, MqttSubscribeMessage mqttSubscribeMessage){
        Long deviceId = mqttChannelApi.getDeviceId(ctx);
        ConcurrentSkipListSet<String> channelTopics = mqttChannelApi.getChannel(ctx).getTopics();
        int num = mqttSubscribeMessage.payload().topicSubscriptions().size();
        List<Integer> grantedQoSLevels = new ArrayList<>(num);
        Date date = new Date();
        for (int i = 0; i < num; i++) {
            grantedQoSLevels.add(mqttSubscribeMessage.payload().topicSubscriptions().get(i).qualityOfService().value());
            //
            String topicName = mqttSubscribeMessage.payload().topicSubscriptions().get(i).topicName();
            ConcurrentHashMap<Long, MqttSubject> mqttTopics = mqttTopicApi.get(topicName);
            if( mqttTopics == null ){
                mqttTopics = new ConcurrentHashMap<>();
            }
            MqttSubject mqttTopic = new MqttSubject();
            mqttTopic.setSubjectName(topicName);
            mqttTopic.setMqttQoS(mqttSubscribeMessage.payload().topicSubscriptions().get(i).qualityOfService());
            mqttTopic.setCtx(ctx);
            mqttTopics.put(deviceId, mqttTopic);
            mqttTopicApi.put(topicName, mqttTopics);
            /**
             * 通道中保存订阅的主题
             */
            channelTopics.add(topicName);
        }
        mqttMessageApi.SUBACK(ctx, grantedQoSLevels, mqttSubscribeMessage.variableHeader().messageId(), mqttSubscribeMessage.fixedHeader().isDup());
    }

    public void replyUNSUBSCRIBE(ChannelHandlerContext ctx, MqttUnsubscribeMessage mqttUnsubscribeMessage){
        Long deviceId = mqttChannelApi.getDeviceId(ctx);
        int num = mqttUnsubscribeMessage.payload().topics().size();
        List<String> topicNames = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            String topicName = mqttUnsubscribeMessage.payload().topics().get(i);
            mqttTopicApi.remove(deviceId, topicName);
            /**
             * 持久化到数据库
             */
            topicNames.add(topicName);
        }
//        subscribeService.deleteBatch(mqttChannelApi.getChannelDbId(ctx), topicNames);
        mqttMessageApi.UNSUBACK(ctx, mqttUnsubscribeMessage.variableHeader().messageId());
    }

    public void broadcastWILL(ChannelHandlerContext ctx){
        MqttChannel mqttChannel = mqttChannelApi.getChannel(ctx);
        if( mqttChannel != null ){
            MqttWill mqttWill = mqttChannel.getMqttWill();
            if( mqttWill != null ){
                String willTopic = mqttWill.getWillTopic();
                byte[] willMessage = mqttWill.getWillMessage();
                MqttQoS mqttQoS = mqttWill.getMqttQoS();
                ConcurrentHashMap<Long, MqttSubject> mqttTopics = mqttTopicApi.get(willTopic);
                for (Long key : mqttTopics.keySet()){
                    int messageId = MessageIdUtil.messageId();
                    MqttSubject mqttTopic = mqttTopics.get(key);
                    switch (mqttWill.getMqttQoS()){
                        case AT_MOST_ONCE:
                            break;
                        /**
                         * 1和2服务质量都需要将报文保存下来
                         */
                        case AT_LEAST_ONCE:
                        case EXACTLY_ONCE:
                            mqttResendApi.saveSendMessage(mqttTopic.getCtx(),
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
                log.info("客户端（"+mqttChannelApi.getDeviceId(ctx)+"）遗愿未设置");
            }
        }else{
            log.error("客户端（"+mqttChannelApi.getDeviceId(ctx)+"）通道不存在");
        }
    }

    public void broadcastPUBLISH(ChannelHandlerContext ctx, String topicName, byte[] payload){
        /**
         * 遍历所有订阅了该主题的客户端
         */
        ConcurrentHashMap<Long, MqttSubject> mqttTopics = mqttTopicApi.get(topicName);
        if( mqttTopics != null ) {
            for (Long deviceId : mqttTopics.keySet()) {
                MqttSubject mqttTopic = mqttTopics.get(deviceId);
                if (mqttTopic.getCtx().channel().isActive() && mqttTopic.getCtx().channel().isWritable()) {
                    int messageId = MessageIdUtil.messageId();
                    if( mqttTopic.getMqttQoS().value() > MqttQoS.AT_MOST_ONCE.value() ) {
                        mqttResendApi.saveSendMessage(mqttTopic.getCtx(), messageId, topicName, payload, mqttTopic.getMqttQoS(), MqttMessageStateConst.PUB);
                    }
                    mqttMessageApi.PUBLISH(mqttTopic.getCtx(), topicName, payload, messageId, mqttTopic.getMqttQoS(), false);
                }
            }
        }
    }
}
