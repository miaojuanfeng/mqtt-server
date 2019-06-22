package com.krt.mqtt.server.entity;

import java.util.Date;

public class Message {
    private Integer id;
    private String deviceId;
    private Integer messageId;
    private String topicName;
    private String topicContent;
    private Integer inserter;
    private Date insertTime;

    public Message(String deviceId, Integer messageId, String topicName, String topicContent, Integer inserter) {
        this.deviceId = deviceId;
        this.messageId = messageId;
        this.topicName = topicName;
        this.topicContent = topicContent;
        this.inserter = inserter;
        this.insertTime = new Date();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Integer getMessageId() {
        return messageId;
    }

    public void setMessageId(Integer messageId) {
        this.messageId = messageId;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getTopicContent() {
        return topicContent;
    }

    public void setTopicContent(String topicContent) {
        this.topicContent = topicContent;
    }

    public Integer getInserter() {
        return inserter;
    }

    public void setInserter(Integer inserter) {
        this.inserter = inserter;
    }

    public Date getInsertTime() {
        return insertTime;
    }

    public void setInsertTime(Date insertTime) {
        this.insertTime = insertTime;
    }
}