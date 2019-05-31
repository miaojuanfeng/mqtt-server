package com.krt.mqtt.server.mapper;

import com.krt.mqtt.server.entity.Message;
import com.krt.mqtt.server.entity.Subscribe;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SubscribeMapper {

    void insertBatch(List<Subscribe> subscribes);

    void deleteBatch(@Param("deviceId") Integer deviceId, @Param("list") List<String> topicNames);
}