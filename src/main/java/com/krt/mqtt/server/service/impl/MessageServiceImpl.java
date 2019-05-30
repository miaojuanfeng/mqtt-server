package com.krt.mqtt.server.service.impl;

import com.krt.mqtt.server.entity.Device;
import com.krt.mqtt.server.entity.Message;
import com.krt.mqtt.server.mapper.DeviceMapper;
import com.krt.mqtt.server.mapper.MessageMapper;
import com.krt.mqtt.server.service.DeviceService;
import com.krt.mqtt.server.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Override
    public void insert(Integer deviceId, Integer messageId, String topicName, byte[] topicMessage) {
        Date date = new Date();
        Message message = new Message();
        message.setDeviceId(deviceId);
        message.setMessageId(messageId);
        message.setTopicName(topicName);
        message.setTopicMessage(new String(topicMessage));
        message.setInsertTime(date);
        message.setUpdateTime(date);
        messageMapper.insert(message);
    }
}
