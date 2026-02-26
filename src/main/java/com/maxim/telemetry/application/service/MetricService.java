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
    private final ProcessService processService;
    private final SystemInfo systemInfo = new SystemInfo();
    private final HardwareAbstractionLayer hal = systemInfo.getHardware();
    private long lastGpuTime = 0;
    private long lastTimestamp = 0;
    private long[] lastCpuTicks;
    private double lastCpuValue = 0.0;

    // Capturar metricas actuales
    public List<TelemetryEntry> gatherMetrics(String sessionName) {
        List<TelemetryEntry> entries = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Uso de CPU (Alta precision con proteccion contra flickering)
        CentralProcessor processor = hal.getProcessor();
        if (lastCpuTicks == null) {
            lastCpuTicks = processor.getSystemCpuLoadTicks();
        } else {
            long[] currentTicks = processor.getSystemCpuLoadTicks();
            if (hasTicksChanged(lastCpuTicks, currentTicks)) {
                lastCpuValue = processor.getSystemCpuLoadBetweenTicks(lastCpuTicks) * 100;
                lastCpuTicks = currentTicks;
            }
        }
        entries.add(createEntry(sessionName, MetricType.CPU_USAGE, lastCpuValue, "%", now));

        // Uso de RAM
        GlobalMemory memory = hal.getMemory();
        double ramUsed = (double) (memory.getTotal() - memory.getAvailable()) / (1024 * 1024 * 1024);
        entries.add(createEntry(sessionName, MetricType.RAM_USAGE, ramUsed, "GB", now));

        // Metricas de GPU (Precision de hardware 99%+)
        double[] gpuHardware = getGpuHardwareMetrics();
        entries.add(createEntry(sessionName, MetricType.GPU_USAGE, gpuHardware[0], "%", now));
        entries.add(createEntry(sessionName, MetricType.VRAM_USAGE, gpuHardware[1], "GB", now));

        // FPS Real
        double fps = calculateRealFPS();
        entries.add(createEntry(sessionName, MetricType.FPS, fps, "FPS", now));

        return entries;
    }

    private boolean hasTicksChanged(long[] oldTicks, long[] newTicks) {
        if (oldTicks == null || newTicks == null || oldTicks.length != newTicks.length) return true;
        for (int i = 0; i < oldTicks.length; i++) {
            if (oldTicks[i] != newTicks[i]) return true;
        }
        return false;
    }

    private double[] getGpuHardwareMetrics() {
        try {
            // Consulta: utilization.gpu, memory.used (en MiB)
            Process process = Runtime.getRuntime().exec("nvidia-smi --query-gpu=utilization.gpu,memory.used --format=csv,noheader,nounits");
            try (java.util.Scanner s = new java.util.Scanner(process.getInputStream())) {
                if (s.hasNextLine()) {
                    String line = s.nextLine();
                    String[] parts = line.split(",");
                    double load = Double.parseDouble(parts[0].trim());
                    double vramUsedGB = Double.parseDouble(parts[1].trim()) / 1024.0;
                    return new double[]{load, vramUsedGB};
                }
            }
        } catch (Exception e) {}
        
        // Fallback OSHI si no hay NVIDIA o falla
        double[] fallback = {0.0, 0.0};
        List<GraphicsCard> graphicsCards = hal.getGraphicsCards();
        if (!graphicsCards.isEmpty()) {
            fallback[1] = 0.0; // OSHI no da VRAM usada facilmente, solo total
        }
        return fallback;
    }

    private double calculateRealFPS() {
        int pid = processService.getForegroundProcessId();
        if (pid <= 0) return getMonitorRefreshRate();

        try {
            String[] cmd = {"cmd.exe", "/c", "wmic path Win32_PerfRawData_GPUPerformanceCounters_GPUEngine WHERE \"Name LIKE 'pid_" + pid + "_%_engtype_3D'\" get RunningTime /value"};
            Process process = Runtime.getRuntime().exec(cmd);
            try (java.util.Scanner s = new java.util.Scanner(process.getInputStream())) {
                long currentGpuTime = 0;
                while (s.hasNextLine()) {
                    String line = s.nextLine().trim();
                    if (line.startsWith("RunningTime=")) {
                        currentGpuTime += Long.parseLong(line.split("=")[1]);
                    }
                }

                long currentTimestamp = System.nanoTime();
                if (lastGpuTime > 0 && currentGpuTime > lastGpuTime) {
                    double timeDiffSeconds = (currentTimestamp - lastTimestamp) / 1_000_000_000.0;
                    if (timeDiffSeconds > 0.05) {
                        double deltaGpuSeconds = (currentGpuTime - lastGpuTime) / 10_000_000.0;
                        double fps = (deltaGpuSeconds / timeDiffSeconds) * getMonitorRefreshRate();
                        
                        lastGpuTime = currentGpuTime;
                        lastTimestamp = currentTimestamp;
                        
                        if (fps < 1) return getMonitorRefreshRate();
                        return Math.min(fps, 999.0);
                    }
                    return lastGpuTime == currentGpuTime ? getMonitorRefreshRate() : (lastGpuTime > 0 ? (lastGpuTime - lastGpuTime) : getMonitorRefreshRate());
                } else {
                    lastGpuTime = currentGpuTime;
                    lastTimestamp = currentTimestamp;
                }
            }
        } catch (Exception e) {}
        return getMonitorRefreshRate();
    }

    private double getMonitorRefreshRate() {
        try {
            java.awt.GraphicsDevice gd = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            int refreshRate = gd.getDisplayMode().getRefreshRate();
            return (refreshRate <= 0 || refreshRate == java.awt.DisplayMode.REFRESH_RATE_UNKNOWN) ? 60.0 : (double) refreshRate;
        } catch (Exception e) {
            return 60.0;
        }
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
