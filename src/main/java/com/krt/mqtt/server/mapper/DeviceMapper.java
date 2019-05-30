package com.krt.mqtt.server.mapper;

import com.krt.mqtt.server.entity.Device;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface DeviceMapper {

    int insert(Device device);

    Device selectByDeviceId(String deviceId);

    int updateByPrimaryKey(Device device);
}