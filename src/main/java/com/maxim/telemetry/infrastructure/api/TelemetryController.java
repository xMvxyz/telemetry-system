package com.maxim.telemetry.infrastructure.api;

import com.maxim.telemetry.domain.model.TelemetryEntry;
import com.maxim.telemetry.domain.model.MetricType;
import com.maxim.telemetry.application.service.RecordingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/telemetry")
@RequiredArgsConstructor
public class TelemetryController {

    private final TelemetryRepository repository;
    private final RecordingService recordingService;

    @PostMapping("/session/start")
    public String startSession(@RequestParam(defaultValue = "gaming-session") String name) {
        recordingService.startRecording(name);
        return "Recording started: " + name;
    }

    @PostMapping("/session/stop")
    public Map<String, Map<String, Double>> stopSession() {
        recordingService.stopRecording();
        return recordingService.getSummary();
    }

    @GetMapping("/summary")
    public Map<String, Map<String, Double>> getSummary() {
        return recordingService.getSummary();
    }

    @PostMapping
    public TelemetryEntry receiveTelemetry(@RequestBody TelemetryEntry entry) {
        if (entry.getMetricType() == MetricType.CPU_USAGE && entry.getValue() > 90.0) {
            System.out.println("[ALERTA CRÍTICA] Servidor " + entry.getServerName() + " tiene uso de CPU elevado: " + entry.getValue() + "%");
        }
        
        return repository.save(entry);
    }

    @GetMapping
    public List<TelemetryEntry> getAllTelemetry() {
        return repository.findAll();
    }

    @GetMapping("/server/{name}")
    public List<TelemetryEntry> getByServer(@PathVariable String name) {
        return repository.findByServerName(name);
    }
}
