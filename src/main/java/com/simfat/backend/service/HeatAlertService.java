package com.simfat.backend.service;

import com.simfat.backend.dto.HeatAlertRequestDTO;
import com.simfat.backend.dto.HeatAlertResponseDTO;
import java.util.List;

public interface HeatAlertService {

    List<HeatAlertResponseDTO> getAll();

    HeatAlertResponseDTO getById(String id);

    List<HeatAlertResponseDTO> getByRegion(String regionId);

    HeatAlertResponseDTO create(HeatAlertRequestDTO request);

    HeatAlertResponseDTO update(String id, HeatAlertRequestDTO request);

    void delete(String id);
}

