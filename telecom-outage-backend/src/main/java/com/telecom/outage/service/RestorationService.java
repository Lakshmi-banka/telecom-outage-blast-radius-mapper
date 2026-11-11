package com.telecom.outage.service;

import com.telecom.outage.dto.Requests.RestorationCreate;
import com.telecom.outage.entity.*;
import com.telecom.outage.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RestorationService {
    private final RestorationHistoryRepository history;
    private final IncidentRepository incidents;
    private final ComponentRepository components;
    private final UserRepository users;
    private final AlarmRepository alarms;
    private final IncidentAlarmRepository incidentAlarms;
    private final FailureEventRepository failureEvents;

    public RestorationService(RestorationHistoryRepository history, IncidentRepository incidents,
                              ComponentRepository components, UserRepository users,
                              AlarmRepository alarms, IncidentAlarmRepository incidentAlarms,
                              FailureEventRepository failureEvents) {
        this.history = history; this.incidents = incidents; this.components = components;
        this.users = users; this.alarms = alarms; this.incidentAlarms = incidentAlarms;
        this.failureEvents = failureEvents;
    }

    public List<RestorationHistory> all() { return history.findAll(); }
    public RestorationHistory get(Long id) {
        return history.findById(id).orElseThrow(() -> new IllegalArgumentException("Restoration record not found"));
    }
    public List<RestorationHistory> byIncident(Long incidentId) {
        if (!incidents.existsById(incidentId)) throw new IllegalArgumentException("Incident not found");
        return history.findByIncident_IdOrderByPerformedAtDesc(incidentId);
    }

    // Engineer submits the restoration work. This deliberately does NOT change component/alarm/incident state.
    @Transactional
    public RestorationHistory create(RestorationCreate request, String email) {
        Incident incident = incidents.findById(request.incidentId())
            .orElseThrow(() -> new IllegalArgumentException("Incident not found"));
        Component component = request.componentId() == null ? null : components.findById(request.componentId())
            .orElseThrow(() -> new IllegalArgumentException("Component not found"));
        User actor = users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));
        RestorationHistory record = new RestorationHistory();
        record.setIncident(incident); record.setComponent(component); record.setPerformedBy(actor);
        record.setActionDescription(request.actionDescription());
        record.setPreviousStatus(request.previousStatus());
        record.setNewStatus(request.newStatus());
        record.setRestorationStatus("VERIFICATION_PENDING");
        return history.save(record);
    }

    @Transactional
    public RestorationHistory verify(Long id, boolean approved, String notes, String verifierEmail) {
        RestorationHistory record = get(id);
        if (!"VERIFICATION_PENDING".equalsIgnoreCase(record.getRestorationStatus()))
            throw new IllegalArgumentException("Only records awaiting verification can be reviewed");
        User verifier = users.findByEmail(verifierEmail)
            .orElseThrow(() -> new IllegalArgumentException("Verifier not found"));
        if (record.getPerformedBy() != null && record.getPerformedBy().getId().equals(verifier.getId()))
            throw new IllegalArgumentException("The engineer who submitted the restoration cannot approve it");
        if (!approved && (notes == null || notes.isBlank()))
            throw new IllegalArgumentException("Rejection notes are required");

        record.setVerifiedBy(verifier);
        record.setVerifiedAt(LocalDateTime.now());
        record.setVerificationNotes(notes);
        if (!approved) {
            record.setRestorationStatus("VERIFICATION_REJECTED");
            return history.save(record);
        }

        record.setRestorationStatus("COMPLETED");
        if (record.getComponent() != null) {
            Long componentId = record.getComponent().getId();
            boolean otherLiveRealtimeFailure = failureEvents
                .existsByFailedComponent_IdAndEventSourceAndEventStatus(componentId, "REALTIME", "ACTIVE");
            if (!otherLiveRealtimeFailure) record.getComponent().setStatus("UP");
        }

        // Clear only alarms linked to this incident; do not clear unrelated alarms on the same component.
        for (IncidentAlarm link : incidentAlarms.findByIncident_Id(record.getIncident().getId())) {
            Alarm alarm = link.getAlarm();
            if (alarm != null && !"CLEARED".equalsIgnoreCase(alarm.getAlarmStatus())) {
                alarm.setAlarmStatus("CLEARED");
                alarm.setClearedAt(LocalDateTime.now());
            }
        }
        record.getIncident().setIncidentStatus("RESOLVED");
        record.getIncident().setResolvedAt(LocalDateTime.now());
        return history.save(record);
    }
}
