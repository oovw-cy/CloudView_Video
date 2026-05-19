package com.shanyangcode.realtimeservice;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class}, scanBasePackages = {"com.shanyangcode.realtimeservice", "com.shanyangcode.common"})

public class RealtimeServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(RealtimeServiceApplication.class, args);
	}

}
