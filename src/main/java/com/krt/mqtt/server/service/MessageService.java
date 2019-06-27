package com.krt.mqtt.server.service;

import com.krt.mqtt.server.entity.Message;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public interface MessageService {

    void insertBatch(ConcurrentLinkedQueue<Message> messages);

}