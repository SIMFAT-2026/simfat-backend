package com.simfat.backend.service.impl;

import com.simfat.backend.config.OpenEoProperties;
import com.simfat.backend.dto.SyncRunResponseDTO;
import com.simfat.backend.exception.ResourceNotFoundException;
import com.simfat.backend.integration.openeo.OpenEoJobRequest;
import com.simfat.backend.integration.openeo.OpenEoJobSubmissionResult;
import com.simfat.backend.integration.openeo.OpenEoServiceClient;
import com.simfat.backend.model.IndicatorType;
import com.simfat.backend.model.OpenEoIndicatorObservation;
import com.simfat.backend.model.OpenEoJobRun;
import com.simfat.backend.model.Region;
import com.simfat.backend.repository.OpenEoIndicatorObservationRepository;
import com.simfat.backend.repository.OpenEoJobRunRepository;
import com.simfat.backend.repository.RegionRepository;
import com.simfat.backend.service.DashboardSnapshotService;
import com.simfat.backend.service.OpenEoSyncService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class OpenEoSyncServiceImpl implements OpenEoSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenEoSyncServiceImpl.class);

    private static final String SOURCE = "openeo-service";
    private static final Set<String> ACTIVE_JOB_STATUSES = Set.of("accepted", "running");

    private final OpenEoServiceClient openEoServiceClient;
    private final RegionRepository regionRepository;
    private final OpenEoJobRunRepository jobRunRepository;
    private final OpenEoIndicatorObservationRepository observationRepository;
    private final DashboardSnapshotService snapshotService;
    private final OpenEoProperties openEoProperties;

    private final Set<String> inFlightKeys = ConcurrentHashMap.newKeySet();
    private final AtomicLong totalSyncRuns = new AtomicLong(0);
    private final AtomicLong totalSyncSuccess = new AtomicLong(0);
    private final AtomicLong totalSyncErrors = new AtomicLong(0);

    public OpenEoSyncServiceImpl(
        OpenEoServiceClient openEoServiceClient,
        RegionRepository regionRepository,
        OpenEoJobRunRepository jobRunRepository,
        OpenEoIndicatorObservationRepository observationRepository,
        DashboardSnapshotService snapshotService,
        OpenEoProperties openEoProperties
    ) {
        this.openEoServiceClient = openEoServiceClient;
        this.regionRepository = regionRepository;
        this.jobRunRepository = jobRunRepository;
        this.observationRepository = observationRepository;
        this.snapshotService = snapshotService;
        this.openEoProperties = openEoProperties;
    }

    @Scheduled(cron = "${openeo.sync.cron:0 */15 * * * *}")
    public void runScheduledSync() {
        if (!openEoProperties.getSync().isEnabled()) {
            return;
        }
        runSync(null);
    }

    @Override
    public SyncRunResponseDTO runSync(String regionId) {
        LocalDateTime startedAt = LocalDateTime.now();
        totalSyncRuns.incrementAndGet();

        LocalDate periodEnd = LocalDate.now();
        LocalDate periodStart = periodEnd.minusDays(30);
        List<Region> regions = resolveRegions(regionId);

        int acceptedCount = 0;
        int deduplicatedCount = 0;
        int errorCount = 0;

        for (Region region : regions) {
            for (IndicatorType indicator : IndicatorType.values()) {
                String lockKey = buildLockKey(region.getId(), indicator, periodStart, periodEnd);
                long flowStartNs = System.nanoTime();

                if (!inFlightKeys.add(lockKey)) {
                    deduplicatedCount++;
                    continue;
                }

                try {
                    boolean alreadyQueued = jobRunRepository.existsByRegionIdAndIndicatorAndPeriodStartAndPeriodEndAndStatusIn(
                        region.getId(),
                        indicator,
                        periodStart.atStartOfDay(),
                        periodEnd.atTime(23, 59, 59),
                        ACTIVE_JOB_STATUSES
                    );

                    if (alreadyQueued) {
                        deduplicatedCount++;
                        continue;
                    }

                    OpenEoJobSubmissionResult result = openEoServiceClient.submitJob(
                        indicator,
                        new OpenEoJobRequest(region.getId(), periodStart, periodEnd)
                    );

                    OpenEoJobRun jobRun = buildJobRun(region, indicator, periodStart, periodEnd, result);
                    jobRunRepository.save(jobRun);
                    acceptedCount++;

                    if (result.getValue() != null) {
                        upsertObservation(region.getId(), indicator, result);
                        snapshotService.recomputeSnapshot(region.getId());
                    }

                    totalSyncSuccess.incrementAndGet();
                    LOGGER.info(
                        "dashboard_sync regionId={} indicator={} status={} durationMs={}",
                        region.getId(),
                        indicator,
                        jobRun.getStatus(),
                        durationMs(flowStartNs)
                    );
                } catch (Exception ex) {
                    errorCount++;
                    totalSyncErrors.incrementAndGet();
                    LOGGER.error(
                        "dashboard_sync regionId={} indicator={} status=error durationMs={} message={}",
                        region.getId(),
                        indicator,
                        durationMs(flowStartNs),
                        ex.getMessage()
                    );
                } finally {
                    inFlightKeys.remove(lockKey);
                }
            }
        }

        LOGGER.info(
            "dashboard_sync_metrics totalRuns={} success={} errors={} accepted={} deduplicated={} durationMs={}",
            totalSyncRuns.get(),
            totalSyncSuccess.get(),
            totalSyncErrors.get(),
            acceptedCount,
            deduplicatedCount,
            durationMs(startedAt)
        );

        SyncRunResponseDTO response = new SyncRunResponseDTO();
        response.setTriggeredAt(startedAt);
        response.setTotalRegions(regions.size());
        response.setTotalJobsAccepted(acceptedCount);
        response.setTotalDeduplicated(deduplicatedCount);
        response.setTotalErrors(errorCount);
        return response;
    }

    private List<Region> resolveRegions(String regionId) {
        if (regionId == null || regionId.isBlank()) {
            return regionRepository.findAll();
        }
        Region region = regionRepository.findById(regionId)
            .orElseThrow(() -> new ResourceNotFoundException("No existe la region para sync: " + regionId));
        List<Region> regions = new ArrayList<>();
        regions.add(region);
        return regions;
    }

    private OpenEoJobRun buildJobRun(
        Region region,
        IndicatorType indicator,
        LocalDate periodStart,
        LocalDate periodEnd,
        OpenEoJobSubmissionResult result
    ) {
        LocalDateTime now = LocalDateTime.now();
        String status = result.getStatus() == null || result.getStatus().isBlank()
            ? "accepted"
            : result.getStatus().toLowerCase();

        OpenEoJobRun jobRun = new OpenEoJobRun();
        jobRun.setJobId(result.getJobId() != null ? result.getJobId() : UUID.randomUUID().toString());
        jobRun.setRegionId(region.getId());
        jobRun.setIndicator(indicator);
        jobRun.setPeriodStart(periodStart.atStartOfDay());
        jobRun.setPeriodEnd(periodEnd.atTime(23, 59, 59));
        jobRun.setStatus(status);
        jobRun.setRequestedAt(now);
        jobRun.setUpdatedAt(now);
        jobRun.setSource(SOURCE);
        jobRun.setErrorCode(result.getErrorCode());
        jobRun.setErrorMessage(result.getErrorMessage());
        if (isTerminalStatus(status)) {
            jobRun.setFinishedAt(now);
        }
        return jobRun;
    }

    private void upsertObservation(String regionId, IndicatorType indicator, OpenEoJobSubmissionResult result) {
        LocalDateTime observedAt = result.getObservedAt() != null ? result.getObservedAt() : LocalDateTime.now();
        OpenEoIndicatorObservation observation = observationRepository
            .findByRegionIdAndIndicatorAndObservedAt(regionId, indicator, observedAt)
            .orElseGet(OpenEoIndicatorObservation::new);

        observation.setRegionId(regionId);
        observation.setIndicator(indicator);
        observation.setObservedAt(observedAt);
        observation.setValue(result.getValue());
        observation.setUnit(result.getUnit());
        observation.setAoi(result.getAoi());
        observation.setQuality(result.getQuality());
        observation.setSource(SOURCE);
        observation.setIngestedAt(LocalDateTime.now());
        observationRepository.save(observation);
    }

    private boolean isTerminalStatus(String status) {
        return "finished".equals(status) || "completed".equals(status) || "error".equals(status) || "failed".equals(status);
    }

    private String buildLockKey(String regionId, IndicatorType indicator, LocalDate from, LocalDate to) {
        return regionId + "|" + indicator + "|" + from + "|" + to;
    }

    private long durationMs(long startedAtNs) {
        return (System.nanoTime() - startedAtNs) / 1_000_000L;
    }

    private long durationMs(LocalDateTime start) {
        return java.time.Duration.between(start, LocalDateTime.now()).toMillis();
    }
}
