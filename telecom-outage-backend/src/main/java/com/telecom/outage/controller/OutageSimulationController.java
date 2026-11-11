
package com.telecom.outage.controller;

import com.telecom.outage.service.OutageSimulationService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/simulations")
public class OutageSimulationController {

    private final OutageSimulationService simulationService;

    public OutageSimulationController(
            OutageSimulationService simulationService) {
        this.simulationService = simulationService;
    }

    public record SimulationRequest(
            @NotNull Long incidentId,
            @NotEmpty List<String> failedComponentIds,
            @Pattern(regexp = "(?i)bfs|dfs")
            String traversal
    ) {}

    @PostMapping("/outage")
    @PreAuthorize("hasAnyRole('ADMIN','NOC_MANAGER','NOC_ANALYST')")
    public Map<?, ?> simulate(
            @Valid @RequestBody SimulationRequest request) {

        return simulationService.simulate(
                request.incidentId(),
                request.failedComponentIds(),
                request.traversal() == null
                        ? "bfs"
                        : request.traversal()
        );
    }
}
