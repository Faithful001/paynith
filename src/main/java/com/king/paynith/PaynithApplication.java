package com.king.paynith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PaynithApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaynithApplication.class, args);
	}

}
