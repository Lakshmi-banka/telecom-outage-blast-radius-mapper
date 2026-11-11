
package com.telecom.outage.service;

import com.telecom.outage.dto.FailureEventResponse;
import com.telecom.outage.entity.Alarm;
import com.telecom.outage.entity.FailureEvent;
import com.telecom.outage.entity.Incident;
import com.telecom.outage.entity.IncidentAlarm;

import com.telecom.outage.repository.AlarmRepository;
import com.telecom.outage.repository.FailureEventRepository;
import com.telecom.outage.repository.IncidentAlarmRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class FailureEventService {

    private final FailureEventRepository failureEventRepository;
    private final AlarmRepository alarmRepository;
    private final IncidentAlarmRepository incidentAlarmRepository;
    private final IncidentService incidentService;

    @Value("${app.failure-mapping.system-user-email}")
    private String systemUserEmail;

    public FailureEventService(
            FailureEventRepository failureEventRepository,
            AlarmRepository alarmRepository,
            IncidentAlarmRepository incidentAlarmRepository,
            IncidentService incidentService) {

        this.failureEventRepository = failureEventRepository;
        this.alarmRepository = alarmRepository;
        this.incidentAlarmRepository = incidentAlarmRepository;
        this.incidentService = incidentService;
    }

    // Retrieve failure events for the frontend.
    @Transactional(readOnly = true)
    public List<FailureEventResponse> getFailureEvents(
            String eventSource,
            String eventStatus) {

        List<FailureEvent> events;

        if (eventSource != null && eventStatus != null) {
            events = failureEventRepository
                    .findByEventSourceAndEventStatus(
                            eventSource, eventStatus);
        } else {
            events = failureEventRepository.findAll();
        }

        return events.stream()
                .filter(event ->
                        eventSource == null ||
                        event.getEventSource()
                                .equalsIgnoreCase(eventSource))
                .filter(event ->
                        eventStatus == null ||
                        event.getEventStatus()
                                .equalsIgnoreCase(eventStatus))
                .sorted(Comparator.comparing(
                        FailureEvent::getStartedAt,
                        Comparator.nullsLast(
                                Comparator.reverseOrder())))
                .map(this::convertToResponse)
                .toList();
    }

    // Automatically process unlinked simulated failures every 15 seconds.
    @Scheduled(
            fixedDelayString =
                    "${app.failure-mapping.interval-ms:15000}"
    )
    @Transactional
    public void mapUnlinkedSimulatedFailures() {

        List<FailureEvent> events =
                failureEventRepository
                        .findByEventSourceIgnoreCaseAndEventStatusIgnoreCaseAndIncidentIsNullAndAlarmIsNullOrderByStartedAtAsc(
                                "SIMULATED",
                                "ACTIVE"
                        );

        for (FailureEvent event : events) {

            // Skip events that have already been mapped.
            if (event.getIncident() != null ||
                    event.getAlarm() != null) {
                continue;
            }

            try {
                createIncidentAndAlarm(event);

            } catch (RuntimeException ex) {

                System.err.println(
                        "Could not map failure event "
                        + event.getId()
                        + ": "
                        + ex.getMessage()
                );

                throw ex;
            }
        }
    }

    // Create and link an alarm and incident for one failure event.
    private void createIncidentAndAlarm(FailureEvent event) {

        if (event.getFailedComponent() == null) {
            throw new IllegalStateException(
                    "Failed component is missing for event "
                    + event.getId()
            );
        }

        var component = event.getFailedComponent();

        String failureType =
                event.getFailureType() == null
                        ? "NETWORK_FAILURE"
                        : event.getFailureType().toUpperCase();

        // 1. Create alarm.
        Alarm alarm = new Alarm();

        alarm.setComponent(component);
        alarm.setAlarmCode("SIM-ALM-" + event.getId());
        alarm.setAlarmType(failureType);
        alarm.setSeverity(getAlarmSeverity(failureType));

        alarm.setDescription(
                "Automatically generated alarm for component "
                + component.getComponentCode()
                + ". Failure type: "
                + failureType
        );

        alarm.setAlarmStatus("OPEN");
        alarm.setOccurredAt(event.getStartedAt());

        alarm = alarmRepository.save(alarm);

        // 2. Create incident.
        Incident incident =
                incidentService.createFromFailureEvent(
                        component,
                        failureType,
                        systemUserEmail
                );

        // 3. Link alarm and incident.
        IncidentAlarm incidentAlarm = new IncidentAlarm();

        incidentAlarm.setIncident(incident);
        incidentAlarm.setAlarm(alarm);

        incidentAlarmRepository.save(incidentAlarm);

        // 4. Update failure event with both IDs.
        event.setAlarm(alarm);
        event.setIncident(incident);

        failureEventRepository.save(event);

        System.out.println(
                "Successfully mapped failure event "
                + event.getId()
                + " | Component: "
                + component.getComponentCode()
                + " | Incident: "
                + incident.getId()
                + " | Alarm: "
                + alarm.getId()
        );
    }

    // Only use severity values permitted by chk_alarm_severity.
    private String getAlarmSeverity(String failureType) {

        if (failureType == null) {
            return "WARNING";
        }

        return switch (failureType.toUpperCase()) {

            case "POWER_FAILURE",
                 "ROUTER_FAILURE" -> "CRITICAL";

            case "LINK_FAILURE" -> "MAJOR";

            case "HIGH_LATENCY" -> "WARNING";

            default -> "MINOR";
        };
    }

    // Convert entity to DTO for API response.
    private FailureEventResponse convertToResponse(
            FailureEvent event) {

        return new FailureEventResponse(

                event.getId(),

                event.getFailedComponent() != null
                        ? event.getFailedComponent().getId()
                        : null,

                event.getFailedComponent() != null
                        ? event.getFailedComponent().getComponentCode()
                        : null,

                event.getFailedComponent() != null
                        ? event.getFailedComponent().getComponentName()
                        : null,

                event.getIncident() != null
                        ? event.getIncident().getId()
                        : null,

                event.getAlarm() != null
                        ? event.getAlarm().getId()
                        : null,

                event.getEventSource(),
                event.getFailureType(),
                event.getEventStatus(),
                event.getStartedAt(),
                event.getScheduledRecoveryAt(),
                event.getRecoveredAt()
        );
    }
}
