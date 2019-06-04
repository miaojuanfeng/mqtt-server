package com.krt.mqtt.server.thread;

import com.krt.mqtt.server.netty.MqttChannelApi;
import com.krt.mqtt.server.netty.MqttMessageService;
import com.krt.mqtt.server.utils.SpringUtil;

public class AliveThread extends Thread{

    private MqttChannelApi mqttChannelApi;

    private Object lock = new Object();

    private final long timeout = 5000;

    public AliveThread(){
        mqttChannelApi = SpringUtil.getBean(MqttChannelApi.class);
        this.start();
    }

    @Override
    public void run() {
        while (true) {
            synchronized (lock) {
                try {
                    mqttChannelApi.checkAlive();
                    lock.wait(timeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
