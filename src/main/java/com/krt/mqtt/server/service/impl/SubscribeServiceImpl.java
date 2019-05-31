package com.krt.mqtt.server.service.impl;

import com.krt.mqtt.server.entity.Message;
import com.krt.mqtt.server.entity.Subscribe;
import com.krt.mqtt.server.mapper.MessageMapper;
import com.krt.mqtt.server.mapper.SubscribeMapper;
import com.krt.mqtt.server.service.MessageService;
import com.krt.mqtt.server.service.SubscribeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class SubscribeServiceImpl implements SubscribeService {

    @Autowired
    private SubscribeMapper subscribeMapper;

    @Override
    public void insertBatch(List<Subscribe> subscribes) {
        if( subscribes.size() > 0 ) {
            subscribeMapper.insertBatch(subscribes);
        }
    }

    @Override
    public void deleteBatch(Integer deviceId, List<String> topicNames) {
        if( topicNames.size() > 0 ) {
            subscribeMapper.deleteBatch(deviceId, topicNames);
        }
    }
}
