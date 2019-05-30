package com.krt.mqtt.server.mapper;

import com.krt.mqtt.server.entity.Device;
import com.krt.mqtt.server.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface MessageMapper {

    int insert(Message message);
}