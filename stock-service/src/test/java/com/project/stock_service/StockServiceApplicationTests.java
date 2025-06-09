package com.project.stock_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Basic test to ensure the StockServiceApplication context loads successfully.
 * This verifies that all Spring beans are correctly configured and
 * the application can start without errors.
 *
 * This test uses a random port and does not connect to a real database or external services,
 * ensuring isolation from the configurations in application.properties.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test") // Use a test profile to isolate configurations if needed
public class StockServiceApplicationTests {

	/**
	 * This test method simply checks if the Spring application context loads.
	 * If it loads without throwing an exception, the test passes,
	 * indicating that the basic setup of the application is correct.
	 */
	@Test
	void contextLoads() {
		// No explicit assertions needed here.
		// If the context fails to load, the test runner will report an error.
	}
}
