package com.simfat.backend.controller;

import com.simfat.backend.dto.AlertsSummaryDTO;
import com.simfat.backend.dto.ApiResponse;
import com.simfat.backend.dto.CriticalRegionDTO;
import com.simfat.backend.dto.DashboardSummaryDTO;
import com.simfat.backend.dto.LossTrendPointDTO;
import com.simfat.backend.service.DashboardService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryDTO>> getSummary() {
        return ResponseEntity.ok(ApiResponse.ok("Resumen de dashboard obtenido correctamente", dashboardService.getSummary()));
    }

    @GetMapping("/critical-regions")
    public ResponseEntity<ApiResponse<List<CriticalRegionDTO>>> getCriticalRegions() {
        return ResponseEntity.ok(ApiResponse.ok(
            "Regiones criticas obtenidas correctamente",
            dashboardService.getCriticalRegions()
        ));
    }

    @GetMapping("/loss-trend")
    public ResponseEntity<ApiResponse<List<LossTrendPointDTO>>> getLossTrend() {
        return ResponseEntity.ok(ApiResponse.ok("Tendencia de perdida obtenida correctamente", dashboardService.getLossTrend()));
    }

    @GetMapping("/alerts-summary")
    public ResponseEntity<ApiResponse<AlertsSummaryDTO>> getAlertsSummary() {
        return ResponseEntity.ok(ApiResponse.ok(
            "Resumen de alertas obtenido correctamente",
            dashboardService.getAlertsSummary()
        ));
    }
}
