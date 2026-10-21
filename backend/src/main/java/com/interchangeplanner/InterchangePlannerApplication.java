package com.interchangeplanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class InterchangePlannerApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterchangePlannerApplication.class, args);
    }
}
