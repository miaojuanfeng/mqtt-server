package com.krt.mqtt.server.thread;

import com.krt.mqtt.server.netty.MqttChannelApi;
import com.krt.mqtt.server.netty.MqttMessageService;
import com.krt.mqtt.server.utils.SpringUtil;

public class AliveThread extends Thread{

    private MqttChannelApi mqttChannelApi;

    private byte[] lock = new byte[0];

    private final long timeout = 5000;

    public AliveThread(){
        mqttChannelApi = SpringUtil.getBean(MqttChannelApi.class);
    }

    @Override
    public void run() {
        while (true) {
            synchronized (lock) {
                try {
//                    System.out.println("Alive check");
                    mqttChannelApi.checkAlive();
                    lock.wait(timeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
