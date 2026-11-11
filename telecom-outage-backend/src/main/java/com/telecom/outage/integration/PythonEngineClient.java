package com.telecom.outage.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class PythonEngineClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public PythonEngineClient(
            RestClient pythonEngineRestClient,
            ObjectMapper objectMapper) {

        this.restClient = pythonEngineRestClient;
        this.objectMapper = objectMapper;
    }

    // Common method for sending JSON requests to Python
    private Map<?, ?> postJson(String endpoint, Object request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Python request body cannot be null: " + endpoint);
        }

        // Debug logging only. The body actually sent is the object below,
        // serialized by the configured message converter -- NOT this string.
        logOutgoingRequest(endpoint, request);

        Map<?, ?> response = restClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                // ------------------------------------------------------------------
                // Pass the DTO, not a pre-serialized String.
                //
                // .body(someJsonString) routes through StringHttpMessageConverter,
                // which is both pointless (double work) and fragile. Handing the
                // object over lets the Jackson converter set Content-Type, charset
                // and framing consistently.
                // ------------------------------------------------------------------
                .body(request)
                .exchange((clientRequest, clientResponse) -> {

                    HttpStatusCode status = clientResponse.getStatusCode();

                    byte[] rawBody = clientResponse.getBody().readAllBytes();

                    String bodyText = new String(rawBody, StandardCharsets.UTF_8);

                    if (status.isError()) {
                        // Surface Python's validation detail verbatim. FastAPI puts the
                        // offending field path in "loc", which is what you actually need.
                        throw new IllegalStateException(
                                "Python engine rejected the request. endpoint="
                                        + endpoint
                                        + " status=" + status.value()
                                        + " response=" + bodyText);
                    }

                    if (rawBody.length == 0) {
                        throw new IllegalStateException(
                                "Python returned an empty response: " + endpoint);
                    }

                    return objectMapper.readValue(rawBody, Map.class);
                });

        if (response == null) {
            throw new IllegalStateException(
                    "Python returned an empty response: " + endpoint);
        }

        return response;
    }

    private void logOutgoingRequest(String endpoint, Object request) {

        try {
            String jsonRequest = objectMapper.writeValueAsString(request);

            System.out.println(
                    "Sending request to Python endpoint: " + endpoint
                            + " (" + jsonRequest.getBytes(StandardCharsets.UTF_8).length
                            + " UTF-8 bytes)");

            System.out.println("Python request JSON: " + jsonRequest);

        } catch (JsonProcessingException exception) {

            throw new IllegalStateException(
                    "Unable to serialize Python request: " + endpoint,
                    exception);
        }
    }

    // Graph analysis
    public Map<?, ?> analyzeGraph(Object request) {
        return postJson("/graph/analyze", request);
    }

    // BFS blast-radius calculation
    public Map<?, ?> calculateBfs(Object request) {
        return postJson("/blast-radius/bfs", request);
    }

    // DFS blast-radius calculation
    public Map<?, ?> calculateDfs(Object request) {
        return postJson("/blast-radius/dfs", request);
    }

    // Alarm correlation
    public Map<?, ?> correlateAlarms(Object request) {
        return postJson("/alarms/correlate", request);
    }

    // Outage simulation
    public Map<?, ?> simulateOutage(Object request) {
        return postJson("/simulation/outage", request);
    }

    // Python health check
    public Map<?, ?> checkHealth() {

        return restClient.get()
                .uri("/health")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(Map.class);
    }
}