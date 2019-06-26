package com.krt.mqtt.server.constant;

import com.krt.mqtt.server.thread.MessageThread;
import com.krt.mqtt.server.thread.ProcessManageThread;
import org.springframework.context.ApplicationContext;

public class CommonConst {

    public static ApplicationContext APPLICATION_CONTEXT = null;
    /**
     * AES解密秘钥
     */
    public static final String AESKEY = "b0qk7cqwntc0ttqx";

    public static final byte DEVICE_DATA_THREAD_SIZE = 10;

    public static final long DEVICE_DATA_THREAD_TIMEOUT = 5*1000L;

    public static final int DEVICE_DATA_FULL_SIZE = 20;

    public static final MessageThread[] DEVICE_DATA_THREAD_ARRAY = new MessageThread[DEVICE_DATA_THREAD_SIZE];

    public static volatile boolean MESSAGE_THREAD_STOP = false;

    public static volatile boolean PROCESS_THREAD_STOP = false;

    public static final ProcessManageThread PROCESS_MANAGE_THREAD = new ProcessManageThread();

    public static final int MAX_RESEND_COUNT = 10;
}
