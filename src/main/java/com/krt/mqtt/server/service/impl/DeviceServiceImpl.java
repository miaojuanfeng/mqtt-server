package com.krt.mqtt.server.service.impl;

import com.krt.mqtt.server.entity.Device;
import com.krt.mqtt.server.mapper.DeviceMapper;
import com.krt.mqtt.server.service.DeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class DeviceServiceImpl implements DeviceService {

    @Autowired
    private DeviceMapper deviceMapper;

    @Override
    public int doLogin(String deviceId, String ip, Integer port) {
        Date date = new Date();
        Device device = deviceMapper.selectByDeviceId(deviceId);
        if( device == null ) {
            device = new Device();
            device.setDeviceId(deviceId);
            device.setIp(ip);
            device.setPort(port);
            device.setLoginTime(date);
            device.setInsertTime(date);
            device.setUpdateTime(date);
            deviceMapper.insert(device);
        }else {
            Device updateDevice = new Device();
            updateDevice.setId(device.getId());
            updateDevice.setIp(ip);
            updateDevice.setPort(port);
            updateDevice.setLoginTime(date);
            updateDevice.setUpdateTime(date);
            deviceMapper.updateByPrimaryKey(updateDevice);
        }
        return device.getId();
    }

    @Override
    public void doLogout(Integer id) {
        Device updateDevice = new Device();
        updateDevice.setId(id);
        updateDevice.setLogoutTime(new Date());
        deviceMapper.updateByPrimaryKey(updateDevice);
    }
}
