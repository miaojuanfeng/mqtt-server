package com.krt.mqtt.server.entity;


import java.util.Date;

public class DeviceCmd {

    private Integer id;

    private Long deviceId;

    private String topicName;

    private String topicContent;

    private Integer status;

    private Integer inserter;

    private Date insertTime;

    public DeviceCmd(Long deviceId, String topicName, String topicContent, Integer status, Integer inserter, Date insertTime) {
        this.deviceId = deviceId;
        this.topicName = topicName;
        this.topicContent = topicContent;
        this.status = status;
        this.inserter = inserter;
        this.insertTime = insertTime;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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