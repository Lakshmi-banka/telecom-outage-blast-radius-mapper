
package com.telecom.outage.controller;

import com.telecom.outage.service.NetworkAnalysisService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/network")
public class NetworkAnalysisController {

    private final NetworkAnalysisService networkAnalysisService;

    public NetworkAnalysisController(
            NetworkAnalysisService networkAnalysisService) {
        this.networkAnalysisService = networkAnalysisService;
    }

    @GetMapping("/{incidentId}/analysis")
    @PreAuthorize("hasAnyRole('ADMIN','NOC_MANAGER','NOC_ANALYST')")
    public Map<?, ?> analyze(@PathVariable Long incidentId) {
        return networkAnalysisService.analyze(incidentId);
    }
}
