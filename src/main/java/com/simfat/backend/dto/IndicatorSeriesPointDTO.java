package com.simfat.backend.dto;

import java.time.LocalDateTime;

public class IndicatorSeriesPointDTO {

    private LocalDateTime observedAt;
    private Double value;

    public LocalDateTime getObservedAt() {
        return observedAt;
    }

    public void setObservedAt(LocalDateTime observedAt) {
        this.observedAt = observedAt;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }
}
