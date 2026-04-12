package com.simfat.backend.integration.openeo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.simfat.backend.exception.OpenEoClientException;
import com.simfat.backend.model.IndicatorType;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class OpenEoServiceClientImpl implements OpenEoServiceClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenEoServiceClientImpl.class);

    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 250L;

    private final RestClient openEoRestClient;
    private final ObjectMapper objectMapper;

    public OpenEoServiceClientImpl(RestClient openEoRestClient, ObjectMapper objectMapper) {
        this.openEoRestClient = openEoRestClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode getCapabilities() {
        return executeWithRetry(
            () -> openEoRestClient.get()
                .uri("/openeo/capabilities")
                .retrieve()
                .body(JsonNode.class),
            "GET /openeo/capabilities"
        );
    }

    @Override
    public JsonNode getCollections(int limit) {
        return executeWithRetry(
            () -> openEoRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/openeo/collections").queryParam("limit", limit).build())
                .retrieve()
                .body(JsonNode.class),
            "GET /openeo/collections"
        );
    }

    @Override
    public OpenEoJobSubmissionResult submitJob(IndicatorType indicator, OpenEoJobRequest request) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("regionId", request.getRegionId());
        payload.put("periodStart", request.getPeriodStart().toString());
        payload.put("periodEnd", request.getPeriodEnd().toString());

        JsonNode responseBody = executeWithRetry(
            () -> openEoRestClient.post()
                .uri("/jobs/" + indicator.name().toLowerCase())
                .body(payload)
                .retrieve()
                .body(JsonNode.class),
            "POST /jobs/" + indicator.name().toLowerCase()
        );

        return parseSubmissionResult(responseBody);
    }

    private JsonNode executeWithRetry(Supplier<JsonNode> call, String operation) {
        int attempt = 0;
        long backoffMs = INITIAL_BACKOFF_MS;

        while (true) {
            attempt++;
            try {
                return call.get();
            } catch (RestClientResponseException ex) {
                boolean clientError = ex.getStatusCode().is4xxClientError();
                String message = "Error al consumir openeo-service en " + operation + ": " + ex.getMessage();

                if (clientError) {
                    LOGGER.warn(
                        "openeo_client status=client_error operation={} httpStatus={} attempt={}",
                        operation,
                        ex.getStatusCode().value(),
                        attempt
                    );
                    throw new OpenEoClientException(message, ex.getStatusCode().value(), false, ex);
                }

                if (attempt >= MAX_ATTEMPTS) {
                    LOGGER.error(
                        "openeo_client status=server_error_no_retry operation={} httpStatus={} attempts={}",
                        operation,
                        ex.getStatusCode().value(),
                        attempt
                    );
                    throw new OpenEoClientException(message, ex.getStatusCode().value(), true, ex);
                }

                LOGGER.warn(
                    "openeo_client status=retrying operation={} httpStatus={} attempt={} backoffMs={}",
                    operation,
                    ex.getStatusCode().value(),
                    attempt,
                    backoffMs
                );
                sleep(backoffMs);
                backoffMs = backoffMs * 2;
            } catch (ResourceAccessException ex) {
                String message = "Timeout o error de red al consumir openeo-service en " + operation;
                if (attempt >= MAX_ATTEMPTS) {
                    LOGGER.error("openeo_client status=network_error_no_retry operation={} attempts={}", operation, attempt);
                    throw new OpenEoClientException(message, null, true, ex);
                }

                LOGGER.warn(
                    "openeo_client status=network_retry operation={} attempt={} backoffMs={}",
                    operation,
                    attempt,
                    backoffMs
                );
                sleep(backoffMs);
                backoffMs = backoffMs * 2;
            }
        }
    }

    private OpenEoJobSubmissionResult parseSubmissionResult(JsonNode body) {
        OpenEoJobSubmissionResult result = new OpenEoJobSubmissionResult();
        if (body == null || body.isNull()) {
            result.setJobId(UUID.randomUUID().toString());
            result.setStatus("accepted");
            return result;
        }

        result.setJobId(readText(body, "jobId", "job_id", "id", "processId"));
        result.setStatus(defaultText(readText(body, "status", "state"), "accepted"));
        result.setValue(readDouble(body, "value"));
        result.setUnit(readText(body, "unit"));
        result.setAoi(readText(body, "aoi"));
        result.setQuality(readText(body, "quality"));
        result.setErrorCode(readText(body, "errorCode", "error_code"));
        result.setErrorMessage(readText(body, "errorMessage", "error_message", "detail"));
        result.setObservedAt(readDateTime(body, "observedAt", "observed_at", "timestamp"));

        if (result.getJobId() == null || result.getJobId().isBlank()) {
            result.setJobId(UUID.randomUUID().toString());
        }
        return result;
    }

    private String readText(JsonNode body, String... keys) {
        for (String key : keys) {
            JsonNode valueNode = body.get(key);
            if (valueNode != null && !valueNode.isNull() && !valueNode.asText().isBlank()) {
                return valueNode.asText();
            }
        }
        JsonNode resultNode = body.get("result");
        if (resultNode != null && resultNode.isObject()) {
            for (String key : keys) {
                JsonNode valueNode = resultNode.get(key);
                if (valueNode != null && !valueNode.isNull() && !valueNode.asText().isBlank()) {
                    return valueNode.asText();
                }
            }
        }
        return null;
    }

    private Double readDouble(JsonNode body, String key) {
        JsonNode node = body.get(key);
        if (node != null && node.isNumber()) {
            return node.doubleValue();
        }
        JsonNode resultNode = body.get("result");
        if (resultNode != null && resultNode.isObject()) {
            JsonNode nested = resultNode.get(key);
            if (nested != null && nested.isNumber()) {
                return nested.doubleValue();
            }
        }
        return null;
    }

    private LocalDateTime readDateTime(JsonNode body, String... keys) {
        String raw = readText(body, keys);
        if (raw == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String defaultText(String input, String fallback) {
        return (input == null || input.isBlank()) ? fallback : input;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new OpenEoClientException("Proceso de retry interrumpido", null, true, ex);
        }
    }
}
