
package com.telecom.outage.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class PythonIntegrationTestController {

    private final RestClient pythonRestClient;

    public PythonIntegrationTestController(
            RestClient pythonEngineRestClient) {
        this.pythonRestClient = pythonEngineRestClient;
    }

    @GetMapping("/python-health")
    public Map<?, ?> testPythonHealth() {

        return pythonRestClient.get()
                .uri("/health")
                .retrieve()
                .body(Map.class);
    }
}
