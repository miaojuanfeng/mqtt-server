package com.krt.mqtt.server.service;

public interface MessageService {

    void insert(String deviceId, Integer messageId, String topicName, byte[] topicMessage);

}