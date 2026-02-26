package com.maxim.telemetry;

import com.maxim.telemetry.infrastructure.ui.TelemetryOverlay;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TelemetrySystemApplication {
    private static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(TelemetrySystemApplication.class);
        builder.headless(false);
        context = builder.run(args);
        TelemetryOverlay.launchOverlay();
    }

    public static ConfigurableApplicationContext getContext() {
        return context;
    }
}
