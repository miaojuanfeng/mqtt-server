package com.krt.mqtt.server;

import com.krt.mqtt.server.netty.NettyServer;
import com.krt.mqtt.server.utils.SpringUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;

@SpringBootApplication
// 启动时先扫描工具包SpringUtil
@ComponentScan(basePackages={"com.krt.mqtt.server.utils","com.krt.mqtt.server"})
public class MqttServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MqttServerApplication.class, args);
        NettyServer.start();
    }

}
