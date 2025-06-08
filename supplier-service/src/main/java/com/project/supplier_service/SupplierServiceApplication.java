package com.project.supplier_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients; // Enable Feign Clients for this service

@SpringBootApplication
@EnableDiscoveryClient // Enables service registration with Eureka
@EnableFeignClients // Enables scanning for Feign clients within this service (even if not consuming)
public class SupplierServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SupplierServiceApplication.class, args);
    }
}