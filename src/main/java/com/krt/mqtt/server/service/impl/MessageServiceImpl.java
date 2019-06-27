package com.krt.mqtt.server.service.impl;

import com.krt.mqtt.server.entity.Message;
import com.krt.mqtt.server.mapper.MessageMapper;
import com.krt.mqtt.server.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Override
    public void insertBatch(ConcurrentLinkedQueue<Message> messages){
        messageMapper.insertBatch(messages);
    }
}
