package com.maxim.telemetry.application.service;

import com.maxim.telemetry.domain.model.MetricType;
import com.maxim.telemetry.domain.model.TelemetryEntry;
import com.maxim.telemetry.infrastructure.ui.TelemetryOverlay;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class RecordingService {

    private final MetricService metricService;
    private final AtomicBoolean isRecording = new AtomicBoolean(false);
    private String currentSession = "Default";

    public void startRecording(String session) {
        this.currentSession = session;
        this.isRecording.set(true);
        System.out.println("🔴 Sesión iniciada: " + session);
    }

    public void stopRecording() {
        this.isRecording.set(false);
        System.out.println("⏹️ Sesión finalizada: " + currentSession);
        printSummary();
    }

    @Scheduled(fixedRate = 1000)
    public void collect() {
        List<TelemetryEntry> entries = metricService.gatherMetrics(currentSession);
        
        // Update Overlay
        entries.forEach(entry -> {
            String formattedValue = String.format("%.1f %s", entry.getValue(), entry.getUnit());
            TelemetryOverlay.updateMetric(entry.getMetricType(), formattedValue);
        });

        // Update recording status in overlay
        TelemetryOverlay.updateStatus(isRecording.get() ? "🔴 RECORDING" : "IDLE");

        if (isRecording.get()) {
            metricService.saveAll(entries);
        }
    }

    public Map<String, Map<String, Double>> getSummary() {
        Map<String, Map<String, Double>> summary = new HashMap<>();
        for (MetricType type : MetricType.values()) {
            Map<String, Double> stats = new HashMap<>();
            stats.put("avg", metricService.getAverage(currentSession, type));
            stats.put("max", metricService.getMax(currentSession, type));
            stats.put("min", metricService.getMin(currentSession, type));
            summary.put(type.name(), stats);
        }
        return summary;
    }

    private void printSummary() {
        System.out.println("📊 --- RESUMEN DE LA SESIÓN --- 📊");
        getSummary().forEach((metric, stats) -> {
            System.out.printf("%s -> AVG: %.2f | MAX: %.2f | MIN: %.2f%n", 
                metric, stats.get("avg"), stats.get("max"), stats.get("min"));
        });
    }

    public boolean isRecording() {
        return isRecording.get();
    }
}
