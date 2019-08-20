package com.krt.mqtt.server.mapper;

import com.krt.mqtt.server.entity.Device;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface DeviceMapper {

    Device selectByDeviceId(String deviceId);

    int update(Device device);
}