package com.krt.mqtt.server.service;


import com.krt.mqtt.server.entity.DeviceData;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author LiXiang
 * @version 1.0
 * @Description: 设备数据服务接口层
 * @date 2019-04-10 15:19:53
 */
public interface DeviceDataService {

    void insertBatch(ConcurrentLinkedQueue<DeviceData> deviceDatas);
}
