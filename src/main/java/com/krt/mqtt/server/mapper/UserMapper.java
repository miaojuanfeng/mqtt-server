package com.krt.mqtt.server.mapper;

import com.krt.mqtt.server.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface UserMapper {

    User Sel(int id);
}