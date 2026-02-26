package com.maxim.telemetry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TelemetrySystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(TelemetrySystemApplication.class, args);
    }
}
