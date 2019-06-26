package com.krt.mqtt.server.thread;

import com.alibaba.fastjson.JSONObject;
import com.krt.mqtt.server.constant.CommonConst;
import com.krt.mqtt.server.constant.SubjectConst;
import com.krt.mqtt.server.netty.NettyProcessHandler;
import com.krt.mqtt.server.service.MessageService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

@Slf4j
public class ProcessThread extends Thread{

    private NettyProcessHandler nettyProcessHandler;

    private ChannelHandlerContext ctx;

    private MqttMessage mqttMessage;

    private Object lock = new Object();

    private static int threadInitNumber;

    private static synchronized int nextThreadNum() {
        return threadInitNumber++;
    }

    public ProcessThread(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
        super();
        this.nettyProcessHandler = CommonConst.APPLICATION_CONTEXT.getBean(NettyProcessHandler.class);
        this.setName("ProcessThread-" + nextThreadNum());
        this.ctx = ctx;
        this.mqttMessage = mqttMessage;
        this.start();
        log.info("线程（" + this.getName() + "）创建运行");
    }

    public void restart(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
        this.ctx = ctx;
        this.mqttMessage = mqttMessage;
        synchronized (lock) {
            lock.notify();
        }
        log.info("线程（" + this.getName() + "）唤醒运行");
    }

    @Override
    public void run() {
        while (!CommonConst.PROCESS_THREAD_STOP) {
            synchronized (lock) {
                nettyProcessHandler.process(ctx, mqttMessage);
                if( CommonConst.PROCESS_THREAD_STOP ){
                    break;
                }
                if( CommonConst.PROCESS_MANAGE_THREAD.insertThread(this) ) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        log.info("线程（" + this.getName() + "）接收中断信号");
                    }
                }else{
                    break;
                }
            }
        }
        log.info("线程（" + this.getName() + "）退出");
    }
}
