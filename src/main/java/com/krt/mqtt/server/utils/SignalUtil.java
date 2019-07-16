package com.krt.mqtt.server.utils;

import lombok.extern.slf4j.Slf4j;
import sun.misc.Signal;
import sun.misc.SignalHandler;
import sun.rmi.runtime.Log;

@Slf4j
public class SignalUtil {

//    private static final String[] LINUX_SIGNAL = {"TERM", "USR1", "USR2"};
//    private static final String[] LINUX_SIGNAL = {"SEGV", "ILL", "FPE", "BUS", "SYS", "CPU", "FSZ", "ABRT", "INT", "TERM", "HUP", "USR1", "USR2", "QUIT", "BREAK", "TRAP", "PIPE"};
    private static final String[] LINUX_SIGNAL = {"TERM", "USR2"};

//    private static final String[] WINDOWS_SIGNAL = {"INT"};
//    private static final String[] WINDOWS_SIGNAL = {"SEGV", "ILL", "FPE", "ABRT", "INT", "TERM", "BREAK"};
    private static final String[] WINDOWS_SIGNAL = {"SEGV", "ILL", "ABRT", "INT", "TERM"};

    public static void initSignal(SignalHandler handler){
        String os = System.getProperties().getProperty("os.name").toLowerCase();
        log.info("os: "+os);
        if( os.startsWith("win") ) {
            for( String signal : WINDOWS_SIGNAL ) {
                Signal sig = new Signal(signal);
                Signal.handle(sig, handler);
            }
        }else if( os.startsWith("lin") ){
            for( String signal : LINUX_SIGNAL ) {
                Signal sig = new Signal(signal);
                Signal.handle(sig, handler);
            }
        }
    }
}
