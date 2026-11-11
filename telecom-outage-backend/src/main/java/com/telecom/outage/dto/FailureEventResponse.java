package com.telecom.outage.dto;

import java.time.LocalDateTime;

public record FailureEventResponse(
        Long failureEventId,
        Long componentId,
        String componentCode,
        String componentName,
        Long incidentId,
        Long alarmId,
        String eventSource,
        String failureType,
        String eventStatus,
        LocalDateTime startedAt,
        LocalDateTime scheduledRecoveryAt,
        LocalDateTime recoveredAt
) {}
