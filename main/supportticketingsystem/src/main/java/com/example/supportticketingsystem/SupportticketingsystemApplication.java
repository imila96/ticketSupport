package com.example.supportticketingsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SupportticketingsystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(SupportticketingsystemApplication.class, args);
	}
}
