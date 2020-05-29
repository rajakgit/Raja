package com.netguardians.solace.services;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.netguardians.solace")
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}