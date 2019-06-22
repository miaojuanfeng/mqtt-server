package com.krt.mqtt.server.service;

import com.krt.mqtt.server.entity.Message;

import java.util.List;

public interface MessageService {

    void insertBatch(List<Message> messages);

}