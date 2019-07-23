package com.krt.mqtt.server.service.impl;


import com.krt.mqtt.server.entity.DeviceCommand;
import com.krt.mqtt.server.entity.DeviceData;
import com.krt.mqtt.server.mapper.DeviceCommandMapper;
import com.krt.mqtt.server.service.DeviceCommandService;
import com.krt.mqtt.server.service.DeviceDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author LiXiang
 * @version 1.0
 * @Description: 云回复命令服务接口实现层
 * @date 2019-04-12 10:53:00
 */
@Service
public class DeviceCommandServiceImpl implements DeviceCommandService {

    @Autowired
    private DeviceCommandMapper deviceCommandMapper;

    @Override
    public void insertBatch(ConcurrentLinkedQueue<DeviceCommand> deviceCommands) {
        deviceCommandMapper.insertBatch(deviceCommands);
    }
}
