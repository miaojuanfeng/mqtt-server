package com.krt.mqtt.server.beans;

import io.netty.handler.codec.mqtt.MqttQoS;

public class MqttWill {

    private String willTopic;

    private byte[] willMessage;

    private MqttQoS mqttQoS;

    private boolean retain;

    public String getWillTopic() {
        return willTopic;
    }

    public void setWillTopic(String willTopic) {
        this.willTopic = willTopic;
    }

    public byte[] getWillMessage() {
        return willMessage;
    }

    public void setWillMessage(byte[] willMessage) {
        this.willMessage = willMessage;
    }

    public MqttQoS getMqttQoS() {
        return mqttQoS;
    }

    public void setMqttQoS(MqttQoS mqttQoS) {
        this.mqttQoS = mqttQoS;
    }

    public boolean isRetain() {
        return retain;
    }

    public void setRetain(boolean retain) {
        this.retain = retain;
    }
}
