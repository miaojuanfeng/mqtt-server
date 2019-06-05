package com.krt.mqtt.server.service.impl;

import com.krt.mqtt.server.entity.DeviceData;
import com.krt.mqtt.server.mapper.DeviceDataMapper;
import com.krt.mqtt.server.service.DeviceDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class DeviceDataServiceImpl implements DeviceDataService {

    @Autowired
    private DeviceDataMapper deviceDataMapper;

    @Override
    public void insert(String deviceId, byte[] data) {
        DeviceData deviceData = new DeviceData();
        deviceData.setDeviceId(deviceId);
        deviceData.setDeviceData(new String(data));
        deviceData.setInserter(1);
        deviceData.setInsertTime(new Date());
        deviceDataMapper.insert(deviceData);
    }
}
