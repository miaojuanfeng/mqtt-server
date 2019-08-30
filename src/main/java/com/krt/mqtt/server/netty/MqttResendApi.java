package com.krt.mqtt.server.netty;

import com.krt.mqtt.server.beans.MqttChannel;
import com.krt.mqtt.server.beans.MqttSendMessage;
import com.krt.mqtt.server.constant.CommonConst;
import com.krt.mqtt.server.constant.MqttMessageStateConst;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class MqttResendApi {

    @Autowired
    private MqttMessageApi mqttMessageApi;

    @Autowired
    private MqttChannelApi mqttChannelApi;

    @Autowired
    private MqttMessageService mqttMessageService;

    public void saveReplyMessage(ChannelHandlerContext ctx, int messageId, String topicName, byte[] payload, int state){
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

    public void saveSendMessage(ChannelHandlerContext ctx, int messageId, String topicName, byte[] payload, MqttQoS mqttQoS, int state){
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
            mqttMessageService.broadcastPUBLISH(mqttSendMessage.getTopicName(), mqttSendMessage.getPayload());
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
        ConcurrentHashMap<Long, MqttChannel> channels = mqttChannelApi.getChannels();
        for(Long deviceId: channels.keySet()) {
            MqttChannel mqttChannel = channels.get(deviceId);
            ConcurrentHashMap<Integer, MqttSendMessage> replyMessages = channels.get(deviceId).getReplyMessages();
            for (Integer messageId : replyMessages.keySet()) {
                MqttSendMessage mqttSendMessage = replyMessages.get(messageId);
                if (mqttSendMessage.getState() == MqttMessageStateConst.REC) {
                    if (mqttSendMessage.getCtx() != null && mqttSendMessage.getCtx().channel().isActive() && mqttSendMessage.getCtx().channel().isWritable()) {
                        if ( mqttSendMessage.getResendCount() > CommonConst.MAX_RESEND_COUNT){
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
        ConcurrentHashMap<Long, MqttChannel> channels = mqttChannelApi.getChannels();
        for(Long deviceId: channels.keySet()) {
            MqttChannel mqttChannel = channels.get(deviceId);
            ConcurrentHashMap<Integer, MqttSendMessage> sendMessages = channels.get(deviceId).getSendMessages();
            for (Integer messageId : sendMessages.keySet()) {
                MqttSendMessage mqttSendMessage = sendMessages.get(messageId);
                if (mqttSendMessage.getCtx() != null && mqttSendMessage.getCtx().channel().isActive() && mqttSendMessage.getCtx().channel().isWritable()) {
                    if (mqttSendMessage.getState() == MqttMessageStateConst.PUB) {
                        if ( mqttSendMessage.getResendCount() > CommonConst.MAX_RESEND_COUNT ){
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

    public boolean checkExistReplyMessage(ChannelHandlerContext ctx, int messageId){
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
}
