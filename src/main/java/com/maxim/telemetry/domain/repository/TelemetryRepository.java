package com.maxim.telemetry.domain.repository;

import com.maxim.telemetry.domain.model.TelemetryEntry;
import com.maxim.telemetry.domain.model.MetricType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TelemetryRepository extends JpaRepository<TelemetryEntry, Long> {
    // Podemos definir búsquedas personalizadas fácilmente
    List<TelemetryEntry> findByServerName(String serverName);
    List<TelemetryEntry> findByMetricType(MetricType metricType);
}
