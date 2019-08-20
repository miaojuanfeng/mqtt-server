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
    public Integer doLogin(String deviceId, String userName, String password) {
        Device device = deviceMapper.selectByDeviceId(deviceId);
        if( device != null ) {
            if( userName.equals(device.getDeviceCode()) && password.equals(device.getVerifyCode()) ){
                try {
                    if( AesUtil.getAESEncrypt(deviceId, CommonConst.AESKEY).equals(device.getVerifyCode()) ) {
                        return device.getId();
                    }
                    return null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // 开始模拟数据
            //return device.getId();
            // 结束模拟数据
        }
        return null;
    }

    @Override
    public int update(Device device) {
        return deviceMapper.update(device);
    }
}
