package com.krt.mqtt.server.entity;


import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

public class ExistLog {

    private Integer id;

    private String deviceId;

    private Byte state;

    private Date time;

    public ExistLog(String deviceId, Byte state, Date time) {
        this.deviceId = deviceId;
        this.state = state;
        this.time = time;
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

    public Byte getState() {
        return state;
    }

    public void setState(Byte state) {
        this.state = state;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }
}