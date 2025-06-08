package com.project.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders; // Import HttpHeaders
import org.springframework.http.HttpMethod; // Import HttpMethod
import org.springframework.http.HttpStatus; // Import HttpStatus
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    /**
     * Configures a WebFilter to handle CORS (Cross-Origin Resource Sharing)
     * for all incoming requests to the API Gateway.
     * This centralizes CORS policy, preventing issues like "multiple Access-Control-Allow-Origin" headers.
     */
    @Bean
    public WebFilter corsFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            // Get the response headers
            HttpHeaders headers = exchange.getResponse().getHeaders();

            // Set the allowed origin. Replace "http://localhost:3000" with your frontend's actual URL.
            // For production, avoid "*" and list specific origins for better security.
            headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000");

            // Set the allowed HTTP methods
            headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, PATCH, OPTIONS");

            // Set the allowed headers that can be sent with the request (e.g., Authorization, Content-Type)
            headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Authorization, Content-Type");

            // Allow credentials (like cookies or HTTP authentication headers, e.g., Authorization)
            headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");

            // Optional: Set max-age for preflight requests to cache the response for a specified duration (in seconds)
            // This reduces the number of OPTIONS preflight requests
            headers.add(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"); // Cache for 1 hour

            // Handle preflight (OPTIONS) requests immediately.
            // If the request method is OPTIONS, set status to OK and complete the response.
            if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
                exchange.getResponse().setStatusCode(HttpStatus.OK);
                return exchange.getResponse().setComplete();
            }

            // Continue the filter chain for actual requests (GET, POST, etc.)
            return chain.filter(exchange);
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
