package com.maxim.telemetry.application.service;

import com.maxim.telemetry.domain.model.MetricType;
import com.maxim.telemetry.domain.model.TelemetryEntry;
import com.maxim.telemetry.domain.repository.TelemetryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MetricService {

    private final TelemetryRepository repository;
    private final SystemInfo systemInfo = new SystemInfo();
    private final HardwareAbstractionLayer hal = systemInfo.getHardware();

    // Capturar metricas actuales
    public List<TelemetryEntry> gatherMetrics(String sessionName) {
        List<TelemetryEntry> entries = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Uso de CPU
        CentralProcessor processor = hal.getProcessor();
        double cpuLoad = processor.getSystemCpuLoad(1000) * 100;
        entries.add(createEntry(sessionName, MetricType.CPU_USAGE, cpuLoad, "%", now));

        // Uso de RAM
        GlobalMemory memory = hal.getMemory();
        double ramUsed = (double) (memory.getTotal() - memory.getAvailable()) / (1024 * 1024 * 1024);
        entries.add(createEntry(sessionName, MetricType.RAM_USAGE, ramUsed, "GB", now));

        // VRAM GPU
        List<GraphicsCard> graphicsCards = hal.getGraphicsCards();
        if (!graphicsCards.isEmpty()) {
            GraphicsCard gpu = graphicsCards.get(0);
            double vramUsed = (double) gpu.getVRam() / (1024.0 * 1024.0 * 1024.0);
            entries.add(createEntry(sessionName, MetricType.VRAM_USAGE, vramUsed, "GB", now));
        }

        // FPS Mock
        entries.add(createEntry(sessionName, MetricType.FPS, 60.0, "FPS", now));

        return entries;
    }

    public List<TelemetryEntry> saveAll(List<TelemetryEntry> entries) {
        return repository.saveAll(entries);
    }

    private TelemetryEntry createEntry(String serverName, MetricType type, Double value, String unit, LocalDateTime timestamp) {
        return TelemetryEntry.builder()
                .serverName(serverName)
                .metricType(type)
                .value(value)
                .unit(unit)
                .timestamp(timestamp)
                .build();
    }
    
    public Double getAverage(String name, MetricType type) {
        return repository.findByServerName(name).stream()
                .filter(e -> e.getMetricType() == type)
                .mapToDouble(TelemetryEntry::getValue).average().orElse(0.0);
    }

    public Double getMax(String name, MetricType type) {
        return repository.findByServerName(name).stream()
                .filter(e -> e.getMetricType() == type)
                .mapToDouble(TelemetryEntry::getValue).max().orElse(0.0);
    }

    public Double getMin(String name, MetricType type) {
        return repository.findByServerName(name).stream()
                .filter(e -> e.getMetricType() == type)
                .mapToDouble(TelemetryEntry::getValue).min().orElse(0.0);
    }
}
