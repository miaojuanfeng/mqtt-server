package com.krt.mqtt.server.thread;

import com.krt.mqtt.server.netty.MqttMessageService;
import com.krt.mqtt.server.utils.SpringUtil;

public class MessageThread extends Thread {

    private MqttMessageService mqttMessageService;

    private byte[] lock = new byte[0];

    private final long timeout = 1000;

    public MessageThread(){
        mqttMessageService = SpringUtil.getBean(MqttMessageService.class);
    }

    @Override
    public void run() {
        while(true){
            synchronized (lock){
                try {
                    mqttMessageService.resendReplyMessage();
                    lock.wait(timeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
