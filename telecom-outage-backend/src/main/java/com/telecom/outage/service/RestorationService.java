package com.telecom.outage.service;

import com.telecom.outage.dto.Requests.RestorationCreate;
import com.telecom.outage.entity.*;
import com.telecom.outage.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class RestorationService {
    private final RestorationHistoryRepository history;
    private final IncidentRepository incidents;
    private final ComponentRepository components;
    private final UserRepository users;

    public RestorationService(RestorationHistoryRepository history, IncidentRepository incidents,
                              ComponentRepository components, UserRepository users) {
        this.history = history; this.incidents = incidents; this.components = components; this.users = users;
    }

    public List<RestorationHistory> all() { return history.findAll(); }

    public RestorationHistory get(Long id) {
        return history.findById(id).orElseThrow(() -> new IllegalArgumentException("Restoration record not found"));
    }

    public List<RestorationHistory> byIncident(Long incidentId) {
        if (!incidents.existsById(incidentId)) throw new IllegalArgumentException("Incident not found");
        return history.findByIncident_IdOrderByPerformedAtDesc(incidentId);
    }

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
        record.setPreviousStatus(request.previousStatus()); record.setNewStatus(request.newStatus());
        record.setRestorationStatus(request.restorationStatus().toUpperCase());
        if (component != null) component.setStatus(request.newStatus().toUpperCase());
        return history.save(record);
    }

    @Transactional
    public RestorationHistory updateStatus(Long id, String status) {
        RestorationHistory record = get(id);
        record.setRestorationStatus(status.toUpperCase());
        return history.save(record);
    }
}
