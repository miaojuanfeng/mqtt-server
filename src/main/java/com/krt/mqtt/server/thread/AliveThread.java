package com.krt.mqtt.server.thread;

import com.krt.mqtt.server.netty.MqttMessageService;
import com.krt.mqtt.server.utils.SpringUtil;

public class AliveThread extends Thread{

    private MqttMessageService mqttMessageService;

    private byte[] lock = new byte[0];

    private final long timeout = 5000;

    public AliveThread(){
        mqttMessageService = SpringUtil.getBean(MqttMessageService.class);
    }

    @Override
    public void run() {
        while (true) {
            synchronized (lock) {
                try {
//                    System.out.println("Alive check");
                    mqttMessageService.checkAlive();
                    lock.wait(timeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
