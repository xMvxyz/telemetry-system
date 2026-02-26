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
    private final ProcessService processService;
    private final AtomicBoolean isRecording = new AtomicBoolean(false);
    private String currentSession = "Default";

    public void startRecording(String session) {
        // Si no se pasa nombre, usar el juego detectado
        this.currentSession = (session == null || session.equals("manual-session")) 
            ? processService.getActiveWindowTitle() 
            : session;
        this.isRecording.set(true);
        System.out.println("Sesion iniciada: " + currentSession);
    }

    public void stopRecording() {
        if (isRecording.get()) {
            this.isRecording.set(false);
            System.out.println("Sesion finalizada: " + currentSession);
            imprimirResumen();
        }
    }

    @Scheduled(fixedRate = 1000)
    public void collect() {
        try {
            String activeGame = processService.getActiveWindowTitle();
            TelemetryOverlay.updateGame(activeGame);

            List<TelemetryEntry> entries = metricService.gatherMetrics(currentSession);
            
            // Actualizar UI
            entries.forEach(entry -> {
                String valorFormateado = String.format("%.1f %s", entry.getValue(), entry.getUnit());
                TelemetryOverlay.updateMetric(entry.getMetricType(), valorFormateado);
            });

            TelemetryOverlay.updateStatus(isRecording.get() ? "REC" : "IDLE");

            if (isRecording.get()) {
                metricService.saveAll(entries);
            }
        } catch (Exception e) {
            // Silenciar si el overlay no esta listo
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

    private void imprimirResumen() {
        System.out.println("--- RESUMEN DE SESION ---");
        getSummary().forEach((metric, stats) -> {
            System.out.printf("%s -> AVG: %.2f | MAX: %.2f | MIN: %.2f%n", 
                metric, stats.get("avg"), stats.get("max"), stats.get("min"));
        });
    }
}
