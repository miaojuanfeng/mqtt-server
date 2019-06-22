package com.krt.mqtt.server.service.impl;

import com.krt.mqtt.server.entity.DeviceData;
import com.krt.mqtt.server.mapper.DeviceDataMapper;
import com.krt.mqtt.server.service.DeviceDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class DeviceDataServiceImpl implements DeviceDataService {

    @Autowired
    private DeviceDataMapper deviceDataMapper;

    @Override
    public void insertBatch(List<DeviceData> deviceData){
        deviceDataMapper.insertBatch(deviceData);
    }
}
