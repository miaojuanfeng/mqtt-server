package com.krt.mqtt.server.entity;

import java.util.Date;

public class DeviceData{

    private Integer id;

    private String deviceId;

    private String dataType;

    private String deviceData;

    private Integer inserter;

    private Date insertTime;

    private Integer updater;

    private Date updateTime;

    public DeviceData(String deviceId, String deviceData, Integer inserter, Date insertTime) {
        this.deviceId = deviceId;
        this.deviceData = deviceData;
        this.inserter = inserter;
        this.insertTime = insertTime;
        this.updateTime = insertTime;
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

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
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

    public Integer getUpdater() {
        return updater;
    }

    public void setUpdater(Integer updater) {
        this.updater = updater;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}