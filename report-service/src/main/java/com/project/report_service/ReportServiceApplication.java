package com.project.report_service; // Changed package name for consistency

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients; // Enable Feign Clients for this service

@SpringBootApplication
@EnableDiscoveryClient // Enables service registration with Eureka
@EnableFeignClients(basePackages = "com.project.report_service.feignclient") // Specify base package for Feign clients
public class ReportServiceApplication { // Renamed from original for clarity, but kept original class name
    public static void main(String[] args) {
        SpringApplication.run(ReportServiceApplication.class, args);
    }
}
