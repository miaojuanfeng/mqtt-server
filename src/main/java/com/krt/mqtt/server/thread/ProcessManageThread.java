package com.krt.mqtt.server.thread;

import com.krt.mqtt.server.constant.CommonConst;
import lombok.extern.slf4j.Slf4j;

import javax.security.auth.Subject;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
public class ProcessManageThread extends Thread{

    private final ConcurrentLinkedQueue<Subject> subjectQueue = new ConcurrentLinkedQueue<>();

    private final ConcurrentLinkedQueue<ProcessThread> freeThreadQueue = new ConcurrentLinkedQueue<>();

    private final ConcurrentLinkedQueue<ProcessThread> usedThreadQueue = new ConcurrentLinkedQueue<>();

    private Object lock = new Object();

    private int threadCount = 0;

    public ProcessManageThread(){
        this.setName("ProcessManageThread");
        this.start();
        log.info(this.getName()+" start.");
    }

    public void insertSubject(String subjectName, String subjectContent){
        subjectQueue.add(new Subject(subjectName, subjectContent, new Date()));
        log.info("插入处理队列");
//        synchronized (lock) {
//            lock.notify();
//        }
    }

    public void doProcess(){
        log.info(this.getName()+" doProcess.");
        ProcessThread processThread = freeThreadQueue.poll();
        Subject subject = subjectQueue.poll();
        if( processThread == null ){
            processThread = new ProcessThread(threadCount, subject.getSubjectName(), subject.getSubjectContent());
            threadCount++;
        }else {
            processThread.restart(subject.getSubjectName(), subject.getSubjectContent());
        }
        usedThreadQueue.add(processThread);
    }

    public boolean insertThread(ProcessThread processThread) {
        log.info("usedThreadQueue("+processThread.getName()+") remove.");
        usedThreadQueue.remove(processThread);
        freeThreadQueue.add(processThread);
        return true;
    }

    @Override
    public void run() {
        while (!CommonConst.processThreadStop){
            synchronized (lock){
                while ( subjectQueue.size() > 0 ){
                    doProcess();
                }
                try {
                    lock.wait(10);
                } catch (InterruptedException e) {
                    while ( subjectQueue.size() > 0 ){
                        doProcess();
                    }
                    CommonConst.processThreadStop = true;
                    log.info("processThreadStop: " + CommonConst.processThreadStop);
                    log.info("usedThreadQueue.size: " + usedThreadQueue.size());
                    while( usedThreadQueue.size() > 0 ) {
                        ProcessThread processThread = usedThreadQueue.poll();
                        log.info("线程（" + processThread.getName() + "）xxx");
                        try {
                            processThread.notifyThread();
                            processThread.join();
                        } catch (InterruptedException ee) {
                            log.info("Join等待线程（" + processThread.getName() + "）退出发生中断");
                            e.printStackTrace();
                        }
                    }
                    e.printStackTrace();
                    log.info("线程（"+this.getName()+"）接收中断信号");
                }
            }
        }
        log.info("线程（"+this.getName()+"）结束");
    }

    public class Subject {

        private String subjectName;

        private String subjectContent;

        private Date date;

        public Subject(String subjectName, String subjectContent, Date date) {
            this.subjectName = subjectName;
            this.subjectContent = subjectContent;
            this.date = date;
        }

        public String getSubjectName() {
            return subjectName;
        }

        public void setSubjectName(String subjectName) {
            this.subjectName = subjectName;
        }

        public String getSubjectContent() {
            return subjectContent;
        }

        public void setSubjectContent(String subjectContent) {
            this.subjectContent = subjectContent;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }
    }
}
