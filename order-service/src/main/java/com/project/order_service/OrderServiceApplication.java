package com.project.order_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients; // Enable Feign Clients for this service

@SpringBootApplication
@EnableDiscoveryClient // Enables service registration with Eureka
@EnableFeignClients(basePackages = "com.project.order_service.feignclient") // Specify base package for Feign clients
public class OrderServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderServiceApplication.class, args);
	}
}

