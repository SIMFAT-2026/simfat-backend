package com.simfat.backend.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.simfat.backend.config.OpenEoProperties;
import com.simfat.backend.dto.SyncRunResponseDTO;
import com.simfat.backend.integration.openeo.OpenEoJobSubmissionResult;
import com.simfat.backend.integration.openeo.OpenEoServiceClient;
import com.simfat.backend.model.DashboardRegionSnapshot;
import com.simfat.backend.model.OpenEoIndicatorObservation;
import com.simfat.backend.model.Region;
import com.simfat.backend.repository.OpenEoIndicatorObservationRepository;
import com.simfat.backend.repository.OpenEoJobRunRepository;
import com.simfat.backend.repository.RegionRepository;
import com.simfat.backend.service.DashboardSnapshotService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpenEoSyncServiceImplTest {

    @Mock
    private OpenEoServiceClient openEoServiceClient;
    @Mock
    private RegionRepository regionRepository;
    @Mock
    private OpenEoJobRunRepository jobRunRepository;
    @Mock
    private OpenEoIndicatorObservationRepository observationRepository;
    @Mock
    private DashboardSnapshotService snapshotService;

    private OpenEoSyncServiceImpl syncService;

    @BeforeEach
    void setUp() {
        OpenEoProperties properties = new OpenEoProperties();
        syncService = new OpenEoSyncServiceImpl(
            openEoServiceClient,
            regionRepository,
            jobRunRepository,
            observationRepository,
            snapshotService,
            properties
        );
    }

    @Test
    void runSync_savesJobAndObservation_whenValueComesInline() {
        Region region = new Region();
        region.setId("region-1");
        when(regionRepository.findAll()).thenReturn(List.of(region));
        when(jobRunRepository.existsByRegionIdAndIndicatorAndPeriodStartAndPeriodEndAndStatusIn(
            any(),
            any(),
            any(),
            any(),
            any()
        )).thenReturn(false);
        when(observationRepository.findByRegionIdAndIndicatorAndObservedAt(any(), any(), any())).thenReturn(Optional.empty());
        when(snapshotService.recomputeSnapshot(any())).thenReturn(new DashboardRegionSnapshot());

        OpenEoJobSubmissionResult result = new OpenEoJobSubmissionResult();
        result.setJobId("job-001");
        result.setStatus("completed");
        result.setValue(0.61);
        when(openEoServiceClient.submitJob(any(), any())).thenReturn(result);

        SyncRunResponseDTO response = syncService.runSync(null);

        assertEquals(1, response.getTotalRegions());
        assertEquals(2, response.getTotalJobsAccepted());
        assertEquals(0, response.getTotalErrors());
        verify(jobRunRepository, times(2)).save(any());
        verify(observationRepository, times(2)).save(any(OpenEoIndicatorObservation.class));
        verify(snapshotService, times(2)).recomputeSnapshot(eq("region-1"));
    }

    @Test
    void runSync_skipsWhenAlreadyQueued() {
        Region region = new Region();
        region.setId("region-2");
        when(regionRepository.findAll()).thenReturn(List.of(region));
        when(jobRunRepository.existsByRegionIdAndIndicatorAndPeriodStartAndPeriodEndAndStatusIn(
            any(),
            any(),
            any(),
            any(),
            any()
        )).thenReturn(true);

        SyncRunResponseDTO response = syncService.runSync(null);

        assertEquals(2, response.getTotalDeduplicated());
        assertEquals(0, response.getTotalJobsAccepted());
        verify(openEoServiceClient, never()).submitJob(any(), any());
    }
}
