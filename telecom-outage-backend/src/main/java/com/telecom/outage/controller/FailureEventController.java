
package com.telecom.outage.controller;

import com.telecom.outage.dto.FailureEventResponse;
import com.telecom.outage.service.FailureEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/failure-events")
public class FailureEventController {

    private final FailureEventService failureEventService;

    public FailureEventController(
            FailureEventService failureEventService) {
        this.failureEventService = failureEventService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','NOC_MANAGER','NOC_ANALYST')")
    public ResponseEntity<List<FailureEventResponse>> getFailureEvents(
            @RequestParam(required = false) String eventSource,
            @RequestParam(required = false) String eventStatus) {

        return ResponseEntity.ok(
                failureEventService.getFailureEvents(
                        eventSource, eventStatus));
    }
}
