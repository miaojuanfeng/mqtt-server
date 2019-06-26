package com.krt.mqtt.server;

import com.krt.mqtt.server.constant.CommonConst;
import com.krt.mqtt.server.netty.NettyServer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;

@SpringBootApplication
//// 启动时先扫描工具包SpringUtil
//@ComponentScan(basePackages={"com.krt.mqtt.server.utils","com.krt.mqtt.server"})
public class MqttServerApplication implements ApplicationContextAware {

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        CommonConst.APPLICATION_CONTEXT = applicationContext;
    }

    public static void main(String[] args) {
        SpringApplication.run(MqttServerApplication.class, args);
        CommonConst.APPLICATION_CONTEXT.getBean(NettyServer.class).start();
    }

}
