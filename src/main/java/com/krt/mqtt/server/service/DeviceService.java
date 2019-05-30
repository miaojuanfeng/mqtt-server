package com.krt.mqtt.server.service;

import com.krt.mqtt.server.entity.Device;

public interface DeviceService {

    int doLogin(String deviceId, String ip, Integer port);

    void doLogout(Integer id);

}