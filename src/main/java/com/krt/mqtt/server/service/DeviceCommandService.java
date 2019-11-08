package com.krt.mqtt.server.service;


import com.krt.mqtt.server.entity.DeviceCmd;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author LiXiang
 * @version 1.0
 * @Description: 云回复命令服务接口层
 * @date 2019-04-12 10:52:59
 */
public interface DeviceCommandService {

    void insertBatch(ConcurrentLinkedQueue<DeviceCmd> deviceCommands);
}
