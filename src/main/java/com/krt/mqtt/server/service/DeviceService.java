package com.krt.mqtt.server.service;

import com.krt.mqtt.server.entity.Device;

public interface DeviceService {

    boolean doLogin(String deviceId, String userName, String password);

}