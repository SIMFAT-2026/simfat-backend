package com.simfat.backend.service.impl;

import com.simfat.backend.dto.RegionRequestDTO;
import com.simfat.backend.dto.RegionResponseDTO;
import com.simfat.backend.exception.BadRequestException;
import com.simfat.backend.exception.ResourceNotFoundException;
import com.simfat.backend.model.Region;
import com.simfat.backend.repository.RegionRepository;
import com.simfat.backend.service.RegionService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RegionServiceImpl implements RegionService {

    private final RegionRepository regionRepository;

    public RegionServiceImpl(RegionRepository regionRepository) {
        this.regionRepository = regionRepository;
    }

    @Override
    public List<RegionResponseDTO> getAll() {
        return regionRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public RegionResponseDTO getById(String id) {
        Region region = findRegionById(id);
        return toResponse(region);
    }

    @Override
    public RegionResponseDTO create(RegionRequestDTO request) {
        regionRepository.findByCodigo(request.getCodigo())
            .ifPresent(existing -> {
                throw new BadRequestException("Ya existe una region con el codigo: " + request.getCodigo());
            });

        Region region = new Region();
        applyChanges(region, request);
        return toResponse(regionRepository.save(region));
    }

    @Override
    public RegionResponseDTO update(String id, RegionRequestDTO request) {
        Region existing = findRegionById(id);
        regionRepository.findByCodigo(request.getCodigo())
            .filter(region -> !region.getId().equals(id))
            .ifPresent(region -> {
                throw new BadRequestException("Ya existe otra region con el codigo: " + request.getCodigo());
            });

        applyChanges(existing, request);
        return toResponse(regionRepository.save(existing));
    }

    @Override
    public void delete(String id) {
        Region region = findRegionById(id);
        regionRepository.delete(region);
    }

    private Region findRegionById(String id) {
        return regionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Region no encontrada con id: " + id));
    }

    private void applyChanges(Region region, RegionRequestDTO request) {
        region.setNombre(request.getNombre());
        region.setCodigo(request.getCodigo());
        region.setZona(request.getZona());
        region.setHectareasBosqueReferencia(request.getHectareasBosqueReferencia());
    }

    private RegionResponseDTO toResponse(Region region) {
        RegionResponseDTO dto = new RegionResponseDTO();
        dto.setId(region.getId());
        dto.setNombre(region.getNombre());
        dto.setCodigo(region.getCodigo());
        dto.setZona(region.getZona());
        dto.setHectareasBosqueReferencia(region.getHectareasBosqueReferencia());
        return dto;
    }
}

