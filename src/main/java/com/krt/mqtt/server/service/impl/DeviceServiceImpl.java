package com.krt.mqtt.server.service.impl;

import com.krt.mqtt.server.constant.CommonConst;
import com.krt.mqtt.server.entity.Device;
import com.krt.mqtt.server.mapper.DeviceMapper;
import com.krt.mqtt.server.service.DeviceService;
import com.krt.mqtt.server.utils.AesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;

@Service
public class DeviceServiceImpl implements DeviceService {

    @Autowired
    private DeviceMapper deviceMapper;

    @Override
    public boolean doLogin(String deviceId, String userName, String password) {
        Date date = new Date();
        Device device = deviceMapper.selectByDeviceId(deviceId);
        if( device != null ) {
            if( userName.equals(device.getDeviceCode()) ){
                try {
                    return AesUtil.getAESDecrypt(password, CommonConst.AESKEY).equals(device.getVerifyCode());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }
}
