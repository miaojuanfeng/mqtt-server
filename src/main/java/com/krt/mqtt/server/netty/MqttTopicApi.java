package com.krt.mqtt.server.netty;

import com.krt.mqtt.server.beans.MqttSubject;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class MqttTopicApi {

    /**
     * 所有主题列表，以及所有订阅该主题的通道
     */
    private static ConcurrentHashMap<String, ConcurrentHashMap<String, MqttSubject>> topics = new ConcurrentHashMap<>();

    public ConcurrentHashMap<String, MqttSubject> get(String topicName){
        return topics.get(topicName);
    }

    public void put(String topicName, ConcurrentHashMap<String, MqttSubject> mqttTopics){
        topics.put(topicName, mqttTopics);
    }

    public void remove(String deviceId, String topicName){
        ConcurrentHashMap<String, MqttSubject> mqttTopics = topics.get(topicName);
        if( mqttTopics != null ){
            mqttTopics.remove(deviceId);
            if( mqttTopics.size() == 0 ){
                topics.remove(topicName);
            }
        }
    }
}
