package com.krt.mqtt.server.thread;

import com.alibaba.fastjson.JSONObject;
import com.krt.mqtt.server.constant.CommonConst;
import com.krt.mqtt.server.constant.SubjectConst;
import com.krt.mqtt.server.service.MessageService;
import com.krt.mqtt.server.utils.SpringUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

@Slf4j
public class ProcessThread extends Thread{

    private String subjectName = null;

    private String subjectContent = null;

    private Object lock = new Object();

    public ProcessThread(int i, String subjectName, String subjectContent) {
        super();
        this.setName("ProcessThread-" + i);
        this.subjectName = subjectName;
        this.subjectContent = subjectContent;
        this.start();
        log.info(this.getName()+" ProcessThread.");
    }

    public void restart(String subjectName, String subjectContent) {
        synchronized (lock) {
            this.subjectName = subjectName;
            this.subjectContent = subjectContent;
            lock.notify();
        }
        log.info(this.getName()+" restart.");
    }

    public void notifyThread(){
        log.info(this.getName()+" notifyThread.");
        synchronized (lock){
            lock.notify();
        }
    }

    @Override
    public void run() {
        while (!CommonConst.processThreadStop) {
            synchronized (lock) {
                if( subjectName != null && subjectContent != null ){
                    switch (subjectName){
                        case SubjectConst.SUBJECT_SHADOW:
                            JSONObject obj = JSONObject.parseObject(subjectContent);
                            System.out.println(this.getName()+": "+obj);
//                            try {
//                                Thread.sleep(10000);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
                            break;
                    }
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
