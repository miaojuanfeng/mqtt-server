package com.krt.mqtt.server.utils;

import io.netty.buffer.ByteBuf;

public class MqttUtil {

    public static byte[] readBytes(ByteBuf byteBuf){
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        return bytes;
    }
}
