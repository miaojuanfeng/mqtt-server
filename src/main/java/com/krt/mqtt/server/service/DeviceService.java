package com.krt.mqtt.server.service;

import com.krt.mqtt.server.entity.Device;

public interface DeviceService {

    Integer doLogin(Long deviceId, String userName, String password);

    int update(Device device);

    int offLineAllDevice();
}