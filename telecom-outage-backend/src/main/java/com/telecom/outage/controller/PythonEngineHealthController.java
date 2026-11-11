package com.telecom.outage.controller;

import com.telecom.outage.integration.PythonEngineClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/python")
public class PythonEngineHealthController {

    private final PythonEngineClient pythonEngineClient;

    public PythonEngineHealthController(
            PythonEngineClient pythonEngineClient) {
        this.pythonEngineClient = pythonEngineClient;
    }

    @GetMapping("/health")
    public Map<?, ?> checkPythonEngine() {
        return pythonEngineClient.checkHealth();
    }
}
