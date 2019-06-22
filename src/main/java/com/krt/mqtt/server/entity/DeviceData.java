package com.krt.mqtt.server.entity;

import java.util.Date;

public class DeviceData {
    private Integer id;
    private String deviceId;
    private String deviceData;
    private Integer inserter;
    private Date insertTime;

    public DeviceData(String deviceId, String deviceData) {
        this.deviceId = deviceId;
        this.deviceData = deviceData;
        this.inserter = 1;
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

    public String getDeviceData() {
        return deviceData;
    }

    public void setDeviceData(String deviceData) {
        this.deviceData = deviceData;
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