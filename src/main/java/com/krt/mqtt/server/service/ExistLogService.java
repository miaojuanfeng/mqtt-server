package com.krt.mqtt.server.service;

import com.krt.mqtt.server.entity.DeviceData;
import com.krt.mqtt.server.entity.ExistLog;

import java.util.concurrent.ConcurrentLinkedQueue;

public interface ExistLogService {

    void insertBatch(ConcurrentLinkedQueue<ExistLog> existLogs);
}
