package com.simfat.backend.integration.openeo;

import java.time.LocalDate;

public class OpenEoJobRequest {

    private String regionId;
    private LocalDate periodStart;
    private LocalDate periodEnd;

    public OpenEoJobRequest() {
    }

    public OpenEoJobRequest(String regionId, LocalDate periodStart, LocalDate periodEnd) {
        this.regionId = regionId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }
}
