package com.krt.mqtt.server.service.impl;

import com.krt.mqtt.server.entity.Device;
import com.krt.mqtt.server.mapper.DeviceMapper;
import com.krt.mqtt.server.service.DeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeviceServiceImpl implements DeviceService {

    @Autowired
    private DeviceMapper deviceMapper;

    @Override
    public Device Sel(int id){
        return deviceMapper.Sel(id);
    }
}
