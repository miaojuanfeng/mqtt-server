package com.krt.mqtt.server.thread;

import com.krt.mqtt.server.constant.CommonConst;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
public class ProcessManageThread extends Thread{

    private final ConcurrentLinkedQueue<NettyMessage> messageQueue = new ConcurrentLinkedQueue<>();

    private final ConcurrentLinkedQueue<ProcessThread> freeThreadQueue = new ConcurrentLinkedQueue<>();

    private final ConcurrentLinkedQueue<ProcessThread> usedThreadQueue = new ConcurrentLinkedQueue<>();

    private Object lock = new Object();

    private final int maximumPoolSize = 36;

    private final int corePoolSize = 24;

    private int poolSize = 0;

    private int largestPoolSize = 0;

    public ProcessManageThread(){
        this.setName("ProcessManageThread");
        this.start();
        log.info("线程（" + this.getName()+"）开始运行");
    }

    public void insertMessage(ChannelHandlerContext ctx, MqttMessage mqttMessage, Date insertTime){
        messageQueue.add(new NettyMessage(ctx, mqttMessage, insertTime));
    }

    public boolean insertThread(ProcessThread processThread) {
        usedThreadQueue.remove(processThread);
        if( poolSize <= corePoolSize ) {
            freeThreadQueue.add(processThread);
            log.info("线程（"+processThread.getName()+"）进入休眠");
            return true;
        }
        poolSize--;
        return false;
    }

    private boolean doProcess(){
        if( freeThreadQueue.size() == 0 && poolSize >= maximumPoolSize ){
            log.info("线程数达最大值无法新建：" + poolSize + "/" + maximumPoolSize);
            return false;
        }
        ProcessThread processThread = freeThreadQueue.poll();
        NettyMessage nettyMessage = messageQueue.poll();
        if( processThread == null ){
            processThread = new ProcessThread(nettyMessage.getCtx(), nettyMessage.getMqttMessage(), nettyMessage.insertTime);
            poolSize++;
            if( poolSize > largestPoolSize ){
                largestPoolSize = poolSize;
                log.info("更新最大线程数量：" + largestPoolSize);
            }
        }else {
            processThread.restart(nettyMessage.getCtx(), nettyMessage.getMqttMessage(), nettyMessage.insertTime);
        }
        usedThreadQueue.add(processThread);
        return true;
    }

    @Override
    public void run() {
        while (!CommonConst.PROCESS_THREAD_STOP){
            synchronized (lock){
                while ( messageQueue.size() > 0 ){
                    if( !doProcess() ){
                        break;
                    }
                }
                try {
                    lock.wait(10);
                } catch (InterruptedException e) {
                    while ( messageQueue.size() > 0 ){
                        doProcess();
                    }
                    CommonConst.PROCESS_THREAD_STOP = true;
                    log.info("工作中Process线程数量: " + usedThreadQueue.size());
                    while( usedThreadQueue.size() > 0 ) {
                        ProcessThread processThread = usedThreadQueue.poll();
                        log.info("线程（" + processThread.getName() + "）工作中，等待其结束");
                        try {
                            processThread.join();
                        } catch (InterruptedException ee) {
                            log.info("线程（" + this.getName() + "）等待线程（" + processThread.getName() + "）退出发生中断");
                            e.printStackTrace();
                        }
                    }
                    log.info("线程（"+this.getName()+"）接收中断信号");
                    e.printStackTrace();
                }
            }
        }
        log.info("线程（"+this.getName()+"）退出");
    }

    public class NettyMessage {

        private ChannelHandlerContext ctx;

        private MqttMessage mqttMessage;

        private Date insertTime;

        public NettyMessage(ChannelHandlerContext ctx, MqttMessage mqttMessage, Date insertTime) {
            this.ctx = ctx;
            this.mqttMessage = mqttMessage;
            this.insertTime = insertTime;
        }

        public ChannelHandlerContext getCtx() {
            return ctx;
        }

        public void setCtx(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        public MqttMessage getMqttMessage() {
            return mqttMessage;
        }

        public void setMqttMessage(MqttMessage mqttMessage) {
            this.mqttMessage = mqttMessage;
        }

        public Date getInsertTime() {
            return insertTime;
        }

        public void setInsertTime(Date insertTime) {
            this.insertTime = insertTime;
        }
    }
}
