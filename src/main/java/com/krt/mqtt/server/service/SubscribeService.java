package com.krt.mqtt.server.service;

import com.krt.mqtt.server.entity.Subscribe;

import java.util.List;

public interface SubscribeService {

    void insertBatch(List<Subscribe> subscribes);

    void deleteBatch(Integer deviceId, List<String> topicNames);

}