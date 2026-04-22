package com.simfat.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simfat.backend.dto.ApiResponse;
import com.simfat.backend.dto.CitizenReportPayloadDTO;
import com.simfat.backend.dto.CitizenReportResponseDTO;
import com.simfat.backend.dto.CitizenReportStatusPatchDTO;
import com.simfat.backend.exception.BadRequestException;
import com.simfat.backend.exception.ResourceNotFoundException;
import com.simfat.backend.model.CitizenReport;
import com.simfat.backend.model.CitizenReportStatus;
import com.simfat.backend.repository.CitizenReportRepository;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/citizen-reports")
public class CitizenReportController {

    private final CitizenReportRepository citizenReportRepository;
    private final ObjectMapper objectMapper;

    public CitizenReportController(CitizenReportRepository citizenReportRepository, ObjectMapper objectMapper) {
        this.citizenReportRepository = citizenReportRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CitizenReportResponseDTO>>> getAll(
        @RequestParam(required = false) String regionId,
        @RequestParam(required = false) CitizenReportStatus status,
        @RequestParam(required = false) String category
    ) {
        List<CitizenReportResponseDTO> items = citizenReportRepository.findAll()
            .stream()
            .filter(item -> regionId == null || regionId.isBlank() || regionId.equals(item.getRegionId()))
            .filter(item -> status == null || status == item.getStatus())
            .filter(item -> category == null || category.isBlank() || category.equalsIgnoreCase(item.getCategory()))
            .sorted((a, b) -> {
                LocalDateTime left = a.getCreatedAt() == null ? LocalDateTime.MIN : a.getCreatedAt();
                LocalDateTime right = b.getCreatedAt() == null ? LocalDateTime.MIN : b.getCreatedAt();
                return right.compareTo(left);
            })
            .map(this::toResponse)
            .toList();

        return ResponseEntity.ok(ApiResponse.ok("Reportes ciudadanos obtenidos correctamente", items));
    }

    @PostMapping(consumes = { "multipart/form-data" })
    public ResponseEntity<ApiResponse<CitizenReportResponseDTO>> create(
        @RequestPart("payload") String payload,
        @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        CitizenReportPayloadDTO dto = parsePayload(payload);

        CitizenReport report = new CitizenReport();
        report.setRegionId(dto.getRegionId());
        report.setCategory(dto.getCategory().toUpperCase());
        report.setDescription(dto.getDescription());
        report.setLatitude(dto.getLatitude());
        report.setLongitude(dto.getLongitude());
        report.setStatus(CitizenReportStatus.RECIBIDO);
        report.setCreatedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());

        if (files != null && !files.isEmpty()) {
            report.setPhotos(files.stream().map(MultipartFile::getOriginalFilename).filter(name -> name != null && !name.isBlank()).toList());
        }

        CitizenReport created = citizenReportRepository.save(report);
        return ResponseEntity.ok(ApiResponse.ok("Reporte ciudadano creado correctamente", toResponse(created)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<CitizenReportResponseDTO>> patchStatus(
        @PathVariable String id,
        @Valid @RequestBody CitizenReportStatusPatchDTO patch
    ) {
        CitizenReport existing = citizenReportRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reporte ciudadano no encontrado con id: " + id));

        existing.setStatus(patch.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());

        CitizenReport updated = citizenReportRepository.save(existing);
        return ResponseEntity.ok(ApiResponse.ok("Estado de reporte actualizado correctamente", toResponse(updated)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable String id) {
        CitizenReport existing = citizenReportRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reporte ciudadano no encontrado con id: " + id));

        citizenReportRepository.delete(existing);
        return ResponseEntity.ok(ApiResponse.ok("Reporte ciudadano eliminado correctamente", id));
    }

    private CitizenReportPayloadDTO parsePayload(String rawPayload) {
        try {
            String normalized = rawPayload == null ? "" : rawPayload.trim();
            try {
                return objectMapper.readValue(normalized, CitizenReportPayloadDTO.class);
            } catch (IOException first) {
                String unescaped = objectMapper.readValue(normalized, String.class);
                return objectMapper.readValue(unescaped, CitizenReportPayloadDTO.class);
            }
        } catch (IOException ex) {
            throw new BadRequestException("Payload de reporte ciudadano invalido");
        }
    }

    private CitizenReportResponseDTO toResponse(CitizenReport item) {
        CitizenReportResponseDTO dto = new CitizenReportResponseDTO();
        dto.setId(item.getId());
        dto.setRegionId(item.getRegionId());
        dto.setCategory(item.getCategory());
        dto.setDescription(item.getDescription());
        dto.setLatitude(item.getLatitude());
        dto.setLongitude(item.getLongitude());
        dto.setStatus(item.getStatus());
        dto.setPhotoCount(item.getPhotos() == null ? 0 : item.getPhotos().size());
        dto.setCreatedAt(item.getCreatedAt());
        return dto;
    }
}
