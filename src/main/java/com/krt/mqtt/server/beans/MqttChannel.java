package com.krt.mqtt.server.beans;

import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;

public class MqttChannel {

    private Long deviceId;

    private ChannelHandlerContext ctx;

    private MqttWill mqttWill;

    private int keepAlive;

    private long activeTime;

    /**
     * 订阅的主题
     */
    private ConcurrentSkipListSet<String> topics = new ConcurrentSkipListSet<>();

    /**
     * 未完成的回复报文
     */
    private ConcurrentHashMap<Integer, MqttSendMessage> replyMessages;

    /**
     * 未完成的发送报文
     */
    private ConcurrentHashMap<Integer, MqttSendMessage> sendMessages;

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public void setCtx(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    public MqttWill getMqttWill() {
        return mqttWill;
    }

    public void setMqttWill(MqttWill mqttWill) {
        this.mqttWill = mqttWill;
    }

    public int getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(int keepAlive) {
        this.keepAlive = keepAlive;
    }

    public long getActiveTime() {
        return activeTime;
    }

    public void setActiveTime(long activeTime) {
        this.activeTime = activeTime;
    }

    public ConcurrentSkipListSet<String> getTopics() {
        return topics;
    }

    public void setTopics(ConcurrentSkipListSet<String> topics) {
        this.topics = topics;
    }

    public ConcurrentHashMap<Integer, MqttSendMessage> getReplyMessages() {
        return replyMessages;
    }

    public void setReplyMessages(ConcurrentHashMap<Integer, MqttSendMessage> replyMessages) {
        this.replyMessages = replyMessages;
    }

    public ConcurrentHashMap<Integer, MqttSendMessage> getSendMessages() {
        return sendMessages;
    }

    public void setSendMessages(ConcurrentHashMap<Integer, MqttSendMessage> sendMessages) {
        this.sendMessages = sendMessages;
    }
}
