
package com.telecom.outage.service;

import com.telecom.outage.dto.PythonEngineDtos;
import com.telecom.outage.integration.PythonEngineClient;
import com.telecom.outage.integration.PythonTopologyService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class NetworkAnalysisService {

    private final PythonEngineClient pythonEngineClient;
    private final PythonTopologyService pythonTopologyService;

    public NetworkAnalysisService(
            PythonEngineClient pythonEngineClient,
            PythonTopologyService pythonTopologyService) {

        this.pythonEngineClient = pythonEngineClient;
        this.pythonTopologyService = pythonTopologyService;
    }

    @Transactional(readOnly = true)
    public Map<?, ?> analyze(Long incidentId) {

        PythonEngineDtos.TopologyRequest topology =
                pythonTopologyService.buildTopology(incidentId);

        return pythonEngineClient.analyzeGraph(topology);
    }
}
