package com.krt.mqtt.server.service.impl;

import com.krt.mqtt.server.entity.Message;
import com.krt.mqtt.server.mapper.MessageMapper;
import com.krt.mqtt.server.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Override
    public void insertBatch(List<Message> messages){
        messageMapper.insertBatch(messages);
    }
}
