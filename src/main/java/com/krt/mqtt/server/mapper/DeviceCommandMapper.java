package com.krt.mqtt.server.mapper;

import com.krt.mqtt.server.entity.DeviceCommand;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author LiXiang
 * @version 1.0
 * @Description: 云回复命令映射层
 * @date 2019-04-12 10:52:59
 */
@Mapper
public interface DeviceCommandMapper{

    void insertBatch(@Param("list") ConcurrentLinkedQueue<DeviceCommand> deviceCommands);
}
