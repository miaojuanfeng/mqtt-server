package com.krt.mqtt.server.entity;


import java.util.Date;

public class DeviceCommand {

    private Integer id;

    private Long deviceId;

    private String command;

    private Integer inserter;

    private Date insertTime;

    public DeviceCommand(Long deviceId, String command, Integer inserter, Date insertTime) {
        this.deviceId = deviceId;
        this.command = command;
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

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
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