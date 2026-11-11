
package com.telecom.outage.controller;

import com.telecom.outage.service.AlarmCorrelationService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/alarms")
public class AlarmCorrelationController {

    private final AlarmCorrelationService service;

    public AlarmCorrelationController(
            AlarmCorrelationService service) {
        this.service = service;
    }

    @PostMapping("/correlate")
    @PreAuthorize("hasAnyRole('ADMIN','NOC_MANAGER','NOC_ANALYST')")
    public Map<?, ?> correlate(
            @RequestParam(defaultValue = "10") int windowMinutes,
            @RequestParam(defaultValue = "component") String groupBy) {

        return service.correlateAlarms(windowMinutes, groupBy);
    }
}
