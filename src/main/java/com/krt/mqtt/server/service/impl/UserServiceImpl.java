package com.krt.mqtt.server.service.impl;

import com.krt.mqtt.server.entity.User;
import com.krt.mqtt.server.mapper.UserMapper;
import com.krt.mqtt.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public User Sel(int id){
        return userMapper.Sel(id);
    }
}
