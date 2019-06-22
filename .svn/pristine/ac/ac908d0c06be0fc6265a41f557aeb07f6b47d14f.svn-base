package com.krt.mqtt.server.thread;

import com.krt.mqtt.server.netty.MqttMessageService;
import com.krt.mqtt.server.netty.MqttResendApi;
import com.krt.mqtt.server.utils.SpringUtil;

public class SendMessageThread extends Thread{

    private MqttResendApi mqttResendApi;

    private Object lock = new Object();

    private final long timeout = 1000;

    public SendMessageThread(){
        mqttResendApi = SpringUtil.getBean(MqttResendApi.class);
        this.start();
    }

    @Override
    public void run() {
        while(true){
            synchronized (lock){
                try {
                    mqttResendApi.resendSendMessage();
                    lock.wait(timeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
