package com.krt.mqtt.server.thread;

public class PublishThread extends Thread {

    private byte[] lock = new byte[0];

    private final long timeout = 5000;

    @Override
    public void run() {
        while(true){
            synchronized (lock){
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
