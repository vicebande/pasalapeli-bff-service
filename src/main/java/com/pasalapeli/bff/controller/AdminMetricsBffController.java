package com.pasalapeli.bff.controller;

import com.pasalapeli.bff.dto.MetricasSnapshotDTO;
import com.pasalapeli.bff.metrics.MetricasAggService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint administrativo de metricas de ventas. Queda cubierto por la
 * regla existente {@code /api/admin/** -> hasRole("ADMIN")} de SecurityConfig.
 *
 * Sirve el snapshot inicial que el dashboard consume antes de conectarse
 * al WebSocket (las actualizaciones llegan despues por /topic/metrics).
 */
@RestController
@RequestMapping("/api/admin/metrics")
@RequiredArgsConstructor
public class AdminMetricsBffController {

    private final MetricasAggService metricasAggService;

    @GetMapping("/snapshot")
    public ResponseEntity<MetricasSnapshotDTO> obtenerSnapshot() {
        return ResponseEntity.ok(metricasAggService.snapshot());
    }
}
