package com.krt.mqtt.server.mapper;

import com.krt.mqtt.server.entity.DeviceData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ConcurrentLinkedQueue;

@Mapper
@Repository
public interface DeviceDataMapper{

    void insertBatch(@Param("list") ConcurrentLinkedQueue<DeviceData> deviceDatas);
}
