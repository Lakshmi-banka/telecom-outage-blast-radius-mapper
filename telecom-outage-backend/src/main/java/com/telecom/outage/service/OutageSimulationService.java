
package com.telecom.outage.service;

import com.telecom.outage.dto.PythonEngineDtos;
import com.telecom.outage.integration.PythonEngineClient;
import com.telecom.outage.integration.PythonTopologyService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class OutageSimulationService {

    private final PythonEngineClient pythonEngineClient;
    private final PythonTopologyService pythonTopologyService;

    public OutageSimulationService(
            PythonEngineClient pythonEngineClient,
            PythonTopologyService pythonTopologyService) {

        this.pythonEngineClient = pythonEngineClient;
        this.pythonTopologyService = pythonTopologyService;
    }

    @Transactional(readOnly = true)
    public Map<?, ?> simulate(
            Long incidentId,
            List<String> failedComponentIds,
            String traversal) {

        if (failedComponentIds == null
                || failedComponentIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one failed component is required");
        }

        if (!"bfs".equalsIgnoreCase(traversal)
                && !"dfs".equalsIgnoreCase(traversal)) {
            throw new IllegalArgumentException(
                    "Traversal must be bfs or dfs");
        }

        // Build topology from PostgreSQL
        PythonEngineDtos.TopologyRequest topology =
                pythonTopologyService.buildTopology(incidentId);

        // Prepare simulation request
        PythonEngineDtos.SimulationRequest request =
                new PythonEngineDtos.SimulationRequest(
                        topology.rootComponentId(),
                        topology.components(),
                        topology.dependencies(),
                        topology.services(),
                        failedComponentIds,
                        false,
                        traversal.toLowerCase()
                );

        // Send simulation to Python
        return pythonEngineClient.simulateOutage(request);
    }
}
