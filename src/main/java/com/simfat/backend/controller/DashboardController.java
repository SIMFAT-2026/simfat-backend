package com.simfat.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @GetMapping("/summary")
    public ResponseEntity<Void> getSummary() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @GetMapping("/critical-regions")
    public ResponseEntity<Void> getCriticalRegions() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @GetMapping("/loss-trend")
    public ResponseEntity<Void> getLossTrend() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @GetMapping("/alerts-summary")
    public ResponseEntity<Void> getAlertsSummary() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}

