package com.krt.mqtt.server.netty;

import com.krt.mqtt.server.constant.CommonConst;
import com.krt.mqtt.server.thread.MessageThread;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

@Slf4j
@Component
public class NettyShutdownHandler implements SignalHandler {

    @Override
    public void handle(Signal signal) {
        log.info("接收中断信号: " + signal.getName());
        CommonConst.MESSAGE_THREAD_STOP = true;
        try {
            for(int i = 0; i<CommonConst.DEVICE_DATA_THREAD_SIZE; i++) {
                MessageThread thread = CommonConst.DEVICE_DATA_THREAD_ARRAY[i];
                if( !thread.isInterrupted() ) {
                    thread.interrupt();
                }
                thread.join();
            }
            if( !CommonConst.PROCESS_MANAGE_THREAD.isInterrupted() ) {
                CommonConst.PROCESS_MANAGE_THREAD.interrupt();
            }
            CommonConst.PROCESS_MANAGE_THREAD.join();
        } catch (InterruptedException e) {
            log.info("线程（NettyShutdownHandler）等待线程退出发生中断");
            e.printStackTrace();
        }
        java.lang.Runtime.getRuntime().exit(0);
    }

}
