package com.krt.mqtt.server.service;

import com.krt.mqtt.server.entity.DeviceData;

import java.util.List;

public interface DeviceDataService {

    void insertBatch(List<DeviceData> deviceData);

}