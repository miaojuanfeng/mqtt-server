package com.krt.mqtt.server.thread;

import com.krt.mqtt.server.constant.CommonConst;
import com.krt.mqtt.server.entity.Message;
import com.krt.mqtt.server.service.MessageService;
import com.krt.mqtt.server.utils.SpringUtil;

import java.util.ArrayList;
import java.util.List;

public class DataThread extends Thread{
    /**
     * 同步锁，防止脏读
     */
    private Object lock = new Object();
    /**
     * 缓存数据的容器
     */
    private List<Message> messageQueues;
    /**
     * 数据的最小单元
     */
    private MessageService messageService;

    /**
     * 初始化线程容器 无参构造函数
     */
    public DataThread() {
        super();
    }

    /**
     * 初始化线程容器
     * @param i 指定线程数
     */
    public DataThread(int i) {
        super();
        this.setName("messageThread- " + i);
        this.messageService = SpringUtil.getBean(MessageService.class);
        messageQueues = new ArrayList<>();
        this.start();
    }

    @Override
    public void run() {
        while (true) {
            synchronized (lock) {
                try {
                    persistData();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 将数据缓存到数据库中
     * @param deviceData 需要缓存的数据
     */
    public void insertMessage(Message message) {
        synchronized (lock) {
            messageQueues.add(message);
            if( messageQueues.size() >= CommonConst.DEVICE_DATA_FULL_SIZE ) {
                lock.notify();
            }
        }
    }

    /**
     * 持久化数据方法，将数据保存到数据
     * @throws InterruptedException
     */
    private void persistData() throws InterruptedException {
        if( messageQueues.size() > 0 ) {
            // 将缓存的数据保存到数据库中
            messageService.insertBatch(messageQueues);
            // 清空缓存内容
            messageQueues.clear();
        }
        // 阻塞线程，直到线程超时或者被唤醒
        lock.wait(CommonConst.DEVICE_DATA_THREAD_TIMEOUT);
    }

}