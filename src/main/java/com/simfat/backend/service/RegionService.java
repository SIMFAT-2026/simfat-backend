package com.simfat.backend.service;

import com.simfat.backend.dto.RegionRequestDTO;
import com.simfat.backend.dto.RegionResponseDTO;
import java.util.List;

public interface RegionService {

    List<RegionResponseDTO> getAll();

    RegionResponseDTO getById(String id);

    RegionResponseDTO create(RegionRequestDTO request);

    RegionResponseDTO update(String id, RegionRequestDTO request);

    void delete(String id);
}

