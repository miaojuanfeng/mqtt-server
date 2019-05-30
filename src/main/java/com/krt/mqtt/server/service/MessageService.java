package com.krt.mqtt.server.service;

public interface MessageService {

    void insert(Integer deviceId, Integer messageId, String topicName, byte[] topicMessage);

}