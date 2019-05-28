package com.krt.mqtt.server.beans;

import io.netty.channel.ChannelHandlerContext;

public class MqttChannel {

    private String deviceId;

    private ChannelHandlerContext ctx;

    private MqttWill mqttWill;

    private int keepAlive;

    private long activeTime;

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
}
