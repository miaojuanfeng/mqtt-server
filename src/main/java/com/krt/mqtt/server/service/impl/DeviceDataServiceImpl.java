package com.krt.mqtt.server.service.impl;

import com.krt.mqtt.server.entity.DeviceData;
import com.krt.mqtt.server.mapper.DeviceDataMapper;
import com.krt.mqtt.server.service.DeviceDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author LiXiang
 * @version 1.0
 * @Description: 设备数据服务接口实现层
 * @date 2019-04-10 15:19:53
 */
@Service
public class DeviceDataServiceImpl implements DeviceDataService {

    @Autowired
    private DeviceDataMapper deviceDataMapper;

    @Override
    public void insertBatch(ConcurrentLinkedQueue<DeviceData> deviceDatas) {
        deviceDataMapper.insertBatch(deviceDatas);
    }
}
