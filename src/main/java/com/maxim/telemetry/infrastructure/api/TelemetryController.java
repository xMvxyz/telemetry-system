package com.maxim.telemetry.infrastructure.api;

import com.maxim.telemetry.domain.model.TelemetryEntry;
import com.maxim.telemetry.domain.model.MetricType;
import com.maxim.telemetry.domain.repository.TelemetryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/telemetry")
@RequiredArgsConstructor
public class TelemetryController {

    private final TelemetryRepository repository;

    @PostMapping
    public TelemetryEntry receiveTelemetry(@RequestBody TelemetryEntry entry) {
        // Lógica de procesamiento simple: Alerta si el valor es crítico
        if (entry.getMetricType() == MetricType.CPU_USAGE && entry.getValue() > 90.0) {
            System.out.println("⚠️ [ALERTA CRÍTICA] Servidor " + entry.getServerName() + " tiene uso de CPU elevado: " + entry.getValue() + "%");
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
