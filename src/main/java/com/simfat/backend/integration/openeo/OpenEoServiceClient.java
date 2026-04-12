package com.simfat.backend.integration.openeo;

import com.fasterxml.jackson.databind.JsonNode;
import com.simfat.backend.model.IndicatorType;

public interface OpenEoServiceClient {

    JsonNode getCapabilities();

    JsonNode getCollections(int limit);

    OpenEoJobSubmissionResult submitJob(IndicatorType indicator, OpenEoJobRequest request);
}
