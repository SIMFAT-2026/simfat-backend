package com.simfat.backend.integration.openeo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simfat.backend.exception.OpenEoClientException;
import com.simfat.backend.model.IndicatorType;
import java.io.IOException;
import java.time.LocalDate;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class OpenEoServiceClientImplTest {

    private MockWebServer server;
    private OpenEoServiceClientImpl client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        RestClient restClient = RestClient.builder()
            .baseUrl(server.url("/").toString())
            .build();
        client = new OpenEoServiceClientImpl(restClient, new ObjectMapper());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void submitJob_retriesOnServerErrorAndThenSucceeds() throws InterruptedException {
        server.enqueue(new MockResponse()
            .setResponseCode(500)
            .addHeader("Content-Type", "application/json")
            .setBody("{\"detail\":\"temporary\"}"));
        server.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody("{\"jobId\":\"job-123\",\"status\":\"accepted\"}"));

        OpenEoJobSubmissionResult result = client.submitJob(
            IndicatorType.NDVI,
            new OpenEoJobRequest("region-1", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"))
        );

        assertEquals("job-123", result.getJobId());
        assertEquals("accepted", result.getStatus());
        assertEquals(2, server.getRequestCount());
        assertEquals("/jobs/ndvi", server.takeRequest().getPath());
    }

    @Test
    void submitJob_doesNotRetryOnClientError() {
        server.enqueue(new MockResponse()
            .setResponseCode(400)
            .addHeader("Content-Type", "application/json")
            .setBody("{\"detail\":\"bad request\"}"));

        assertThrows(
            OpenEoClientException.class,
            () -> client.submitJob(
                IndicatorType.NDMI,
                new OpenEoJobRequest("region-1", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"))
            )
        );

        assertEquals(1, server.getRequestCount());
    }

    @Test
    void submitJob_parsesInlineValueWhenAvailable() {
        server.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody("{\"jobId\":\"job-999\",\"status\":\"completed\",\"value\":0.72,\"unit\":\"index\",\"quality\":\"high\"}"));

        OpenEoJobSubmissionResult result = client.submitJob(
            IndicatorType.NDVI,
            new OpenEoJobRequest("region-2", LocalDate.parse("2026-02-01"), LocalDate.parse("2026-02-28"))
        );

        assertEquals("job-999", result.getJobId());
        assertEquals("completed", result.getStatus());
        assertEquals(0.72, result.getValue());
        assertNotNull(result.getUnit());
    }
}
