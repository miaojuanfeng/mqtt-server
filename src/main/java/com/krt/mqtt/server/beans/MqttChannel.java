package com.krt.mqtt.server.beans;

import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.ConcurrentHashMap;

public class MqttChannel {

    private String deviceId;

    private ChannelHandlerContext ctx;

    private MqttWill mqttWill;

    private int keepAlive;

    private long activeTime;

    /**
     * 未完成的回复报文
     */
    private ConcurrentHashMap<Integer, MqttSendMessage> replyMessages;

    /**
     * 未完成的发送报文
     */
    private ConcurrentHashMap<Integer, MqttSendMessage> sendMessages;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
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
