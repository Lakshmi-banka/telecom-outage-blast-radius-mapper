package com.telecom.outage.repository;

import com.telecom.outage.entity.FailureEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FailureEventRepository
        extends JpaRepository<FailureEvent, Long> {

    List<FailureEvent> findByEventSourceAndEventStatus(
            String eventSource,
            String eventStatus
    );

    boolean existsByFailedComponent_IdAndEventSourceAndEventStatus(
            Long componentId,
            String source,
            String status
    );

    // Find simulated events that have not yet been mapped.
    List<FailureEvent>
    findByEventSourceIgnoreCaseAndIncidentIsNullAndAlarmIsNullOrderByStartedAtAsc(
            String eventSource
    );
    List<FailureEvent>
findByEventSourceIgnoreCaseAndEventStatusIgnoreCaseAndIncidentIsNullAndAlarmIsNullOrderByStartedAtAsc(
        String eventSource,
        String eventStatus
);
}