package com.krt.mqtt.server.mapper;

import com.krt.mqtt.server.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface MessageMapper {

    void insertBatch(@Param("list") List<Message> messages);

}