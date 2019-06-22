package com.krt.mqtt.server.netty;

import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

public class NettyShutdownHandler implements SignalHandler {

    @Override
    public void handle(Signal signal) {
        System.out.println("signal name: "+signal.getName());
        java.lang.Runtime.getRuntime().addShutdownHook(new Thread(){
            public void run(){
                System.out.println("saxczxczxczxczzczxc");
            }
        });
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
