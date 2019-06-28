package com.krt.mqtt.server.entity;

import java.util.Date;

public class Message {
    private Integer id;
    private String deviceId;
    private Integer messageId;
    private String subjectName;
    private String subjectContent;
    private Integer inserter;
    private Date insertTime;

    public Message(String deviceId, Integer messageId, String subjectName, String subjectContent, Integer inserter, Date insertTime) {
        this.deviceId = deviceId;
        this.messageId = messageId;
        this.subjectName = subjectName;
        this.subjectContent = subjectContent;
        this.inserter = inserter;
        this.insertTime = insertTime;
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

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getSubjectContent() {
        return subjectContent;
    }

    public void setSubjectContent(String subjectContent) {
        this.subjectContent = subjectContent;
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