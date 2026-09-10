package com.backend.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot Application Entry Point.
 * @SpringBootApplication enables:
 * 1. @Configuration - class can define @Bean definitions.
 * 2. @EnableAutoConfiguration - automatically configures Spring based on jar dependencies (e.g. DataSource, Security).
 * 3. @ComponentScan - scans current package and sub-packages for @Component, @Service, @Repository, @Controller.
 */
@SpringBootApplication
public class BackendProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendProjectApplication.class, args);
    }
}
