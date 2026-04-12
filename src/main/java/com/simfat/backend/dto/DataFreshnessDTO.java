package com.simfat.backend.dto;

import java.time.LocalDateTime;

public class DataFreshnessDTO {

    private String regionId;
    private Long dataFreshnessSeconds;
    private LocalDateTime computedAt;
    private boolean stale;

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public Long getDataFreshnessSeconds() {
        return dataFreshnessSeconds;
    }

    public void setDataFreshnessSeconds(Long dataFreshnessSeconds) {
        this.dataFreshnessSeconds = dataFreshnessSeconds;
    }

    public LocalDateTime getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(LocalDateTime computedAt) {
        this.computedAt = computedAt;
    }

    public boolean isStale() {
        return stale;
    }

    public void setStale(boolean stale) {
        this.stale = stale;
    }
}
