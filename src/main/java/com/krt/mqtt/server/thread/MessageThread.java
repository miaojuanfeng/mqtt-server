package com.krt.mqtt.server.thread;

import com.krt.mqtt.server.constant.CommonConst;
import com.krt.mqtt.server.entity.Message;
import com.krt.mqtt.server.service.MessageService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
public class MessageThread extends Thread{

    private Object lock = new Object();

    private final ConcurrentLinkedQueue<Message> messageQueue = new ConcurrentLinkedQueue<>();

    private MessageService messageService;

    public MessageThread() {
        super();
    }

    public MessageThread(int i) {
        super();
        this.setName("MessageThread-" + i);
        this.messageService = CommonConst.APPLICATION_CONTEXT.getBean(MessageService.class);
        this.start();
    }

    @Override
    public void run() {
        while (!CommonConst.MESSAGE_THREAD_STOP) {
            synchronized (lock) {
                try {
                    persistData();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    log.info("线程（"+this.getName()+"）接收中断信号立即持久数据：" + messageQueue.size());
                    insertBatch();
                }
            }
        }
        log.info("线程（"+this.getName()+"）退出");
    }

    public void insertMessage(Message message) {
        messageQueue.add(message);
        // 限制单次插入数量
        if( messageQueue.size() >= CommonConst.DEVICE_DATA_FULL_SIZE ) {
            synchronized (lock) {
                lock.notify();
            }
        }
    }

    private void persistData() throws InterruptedException {
        insertBatch();
        lock.wait(CommonConst.DEVICE_DATA_THREAD_TIMEOUT);
    }

    private void insertBatch(){
        if( messageQueue.size() > 0 ) {
            messageService.insertBatch(messageQueue);
            messageQueue.clear();
        }
    }

}