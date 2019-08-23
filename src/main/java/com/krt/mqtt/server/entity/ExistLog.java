package com.krt.mqtt.server.entity;


import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

public class ExistLog {

    private Integer id;

    private Long deviceId;

    private Byte state;

    private Date time;

    public ExistLog(Long deviceId, Byte state, Date time) {
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

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
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