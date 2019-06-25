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
        log.info("接收中断信号: "+signal.getName());
//        java.lang.Runtime.getRuntime().addShutdownHook(new Thread(){
//            public void run(){
                CommonConst.messageThreadStop = true;
                log.info("messageThreadStop: " + CommonConst.messageThreadStop);
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
                    log.info("Join等待线程退出发生中断");
                    e.printStackTrace();
                }
//            }
//        });
        java.lang.Runtime.getRuntime().exit(0);
    }

//    private void serialize() {
//        File outputFile = new File(seriablePath, this.getName()+".out");
//        try {
//            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outputFile));
//            out.writeObject(userLtcMapQueues);
//            out.close();
//        }  catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
