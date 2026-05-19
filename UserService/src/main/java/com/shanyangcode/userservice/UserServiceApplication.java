package com.shanyangcode.userservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.shanyangcode.userservice", "com.shanyangcode.common"})
@MapperScan("com.shanyangcode.userservice.mapper")
@EnableFeignClients(basePackages = "com.shanyangcode.userservice.client")
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

}
