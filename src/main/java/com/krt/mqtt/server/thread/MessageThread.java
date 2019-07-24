package com.krt.mqtt.server.thread;

import com.krt.mqtt.server.constant.CommonConst;
import com.krt.mqtt.server.entity.DeviceCommand;
import com.krt.mqtt.server.entity.DeviceData;
import com.krt.mqtt.server.entity.ExistLog;
import com.krt.mqtt.server.service.DeviceCommandService;
import com.krt.mqtt.server.service.DeviceDataService;
import com.krt.mqtt.server.service.ExistLogService;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
public class MessageThread extends Thread{

    private Object lock = new Object();

    private final ConcurrentLinkedQueue<DeviceData> dataQueue = new ConcurrentLinkedQueue<>();

    private final ConcurrentLinkedQueue<DeviceCommand> commandQueue = new ConcurrentLinkedQueue<>();

    private final ConcurrentLinkedQueue<ExistLog> existLogQueue = new ConcurrentLinkedQueue<>();

    private DeviceDataService deviceDataService;

    private DeviceCommandService deviceCommandService;

    private ExistLogService existLogService;

    public MessageThread() {
        super();
    }

    public MessageThread(int i) {
        super();
        this.setName("MessageThread-" + i);
        this.deviceDataService = CommonConst.APPLICATION_CONTEXT.getBean(DeviceDataService.class);
        this.deviceCommandService = CommonConst.APPLICATION_CONTEXT.getBean(DeviceCommandService.class);
        this.existLogService = CommonConst.APPLICATION_CONTEXT.getBean(ExistLogService.class);
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
                    log.info("线程（"+this.getName()+"）接收中断信号立即持久数据：" + dataQueue.size());
                    insertBatch();
                }
            }
        }
        log.info("线程（"+this.getName()+"）退出");
    }

    public void insertDeviceData(DeviceData deviceData) {
        dataQueue.add(deviceData);
        // 限制单次插入数量
        if( dataQueue.size() >= CommonConst.THREAD_DATA_FULL_SIZE ) {
            synchronized (lock) {
                lock.notify();
            }
        }
    }

    public void insertDeviceCommand(DeviceCommand deviceCommand) {
        commandQueue.add(deviceCommand);
        // 限制单次插入数量
        if( commandQueue.size() >= CommonConst.THREAD_DATA_FULL_SIZE ) {
            synchronized (lock) {
                lock.notify();
            }
        }
    }

    public void insertExistLog(ExistLog existLog) {
        existLogQueue.add(existLog);
        // 限制单次插入数量
        if( existLogQueue.size() >= CommonConst.THREAD_DATA_FULL_SIZE ) {
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
        if( dataQueue.size() > 0 ) {
            deviceDataService.insertBatch(dataQueue);
            dataQueue.clear();
        }
        if( commandQueue.size() > 0 ) {
            deviceCommandService.insertBatch(commandQueue);
            commandQueue.clear();
        }
        if( existLogQueue.size() > 0 ) {
            existLogService.insertBatch(existLogQueue);
            existLogQueue.clear();
        }
    }

}