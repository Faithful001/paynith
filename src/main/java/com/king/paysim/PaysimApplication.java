package com.king.paysim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PaysimApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaysimApplication.class, args);
	}

}
