package com.krt.mqtt.server.service.impl;


import com.krt.mqtt.server.entity.DeviceData;
import com.krt.mqtt.server.entity.ExistLog;
import com.krt.mqtt.server.mapper.ExistLogMapper;
import com.krt.mqtt.server.service.ExistLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class ExistLogServiceImpl implements ExistLogService {

    @Autowired
    private ExistLogMapper existLogMapper;

    @Override
    public void insertBatch(ConcurrentLinkedQueue<ExistLog> existLogs) {
        existLogMapper.insertBatch(existLogs);
    }
}
