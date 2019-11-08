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

    public static final byte DEVICE_DATA_THREAD_SIZE = 1<<3;

    public static final long DEVICE_DATA_THREAD_TIMEOUT = 5*1000L;

    public static final int THREAD_DATA_FULL_SIZE = 1<<9;

    public static final MessageThread[] DEVICE_DATA_THREAD_ARRAY = new MessageThread[DEVICE_DATA_THREAD_SIZE];

    public static volatile boolean MESSAGE_THREAD_STOP = false;

    public static volatile boolean PROCESS_THREAD_STOP = false;

    public static ProcessManageThread PROCESS_MANAGE_THREAD;

    public static final int MAX_RESEND_COUNT = 10;

    public static final byte DEVICE_ONLINE = 1;

    public static final byte DEVICE_OFFLINE = 0;

    public static final int DEVICE_STATE_ONLINE = 2;

    public static final int DEVICE_STATE_OFFLINE = 0;

    public static final int MESSAGE_STATUS_OK = 200;

    public static final int MESSAGE_STATUS_ERROR = 500;
}
