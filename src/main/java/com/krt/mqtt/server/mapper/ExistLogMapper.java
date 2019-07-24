package com.krt.mqtt.server.mapper;

import com.krt.mqtt.server.entity.ExistLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ConcurrentLinkedQueue;


@Repository
@Mapper
public interface ExistLogMapper {
    void insertBatch(@Param("list") ConcurrentLinkedQueue<ExistLog> existLogs);
}
