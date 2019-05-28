package com.krt.mqtt.server.beans;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttQoS;

import java.util.Objects;

public class MqttTopic {

    private String topicName;

    private MqttQoS mqttQoS;

    private ChannelHandlerContext ctx;

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public MqttQoS getMqttQoS() {
        return mqttQoS;
    }

    public void setMqttQoS(MqttQoS mqttQoS) {
        this.mqttQoS = mqttQoS;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public void setCtx(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        MqttTopic mqttTopic = (MqttTopic) object;
        return Objects.equals(topicName, mqttTopic.topicName) &&
                mqttQoS == mqttTopic.mqttQoS &&
                Objects.equals(ctx, mqttTopic.ctx);
    }

    @Override
    public int hashCode() {

        return Objects.hash(topicName, mqttQoS, ctx);
    }
}