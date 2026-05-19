package com.shanyangcode.videoactionservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.shanyangcode.videoactionservice", "com.shanyangcode.common"})
@MapperScan("com.shanyangcode.videoactionservice.mapper")
public class VideoActionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(VideoActionServiceApplication.class, args);
    }

}
