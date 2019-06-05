package com.krt.mqtt.server.mapper;

import com.krt.mqtt.server.entity.DeviceData;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface DeviceDataMapper {

    int insert(DeviceData message);
}