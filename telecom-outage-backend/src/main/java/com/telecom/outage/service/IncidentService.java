package com.telecom.outage.service;

import com.telecom.outage.dto.Requests.IncidentCreate;
import com.telecom.outage.entity.Component;
import com.telecom.outage.entity.Incident;
import com.telecom.outage.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class IncidentService {

    private final IncidentRepository incidents;
    private final ComponentRepository components;
    private final UserRepository users;

    public IncidentService(
            IncidentRepository i,
            ComponentRepository c,
            UserRepository u) {

        incidents = i;
        components = c;
        users = u;
    }

    public List<Incident> all() {
        return incidents.findAll();
    }

    public Incident get(Long id) {
        return incidents.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Incident not found"));
    }

    @Transactional
    public Incident create(IncidentCreate r, String email) {

        if (incidents.findByIncidentCode(r.incidentCode()).isPresent()) {
            throw new IllegalArgumentException(
                    "Incident code exists");
        }

        Incident i = new Incident();

        i.setIncidentCode(r.incidentCode());
        i.setTitle(r.title());
        i.setDescription(r.description());
        i.setSeverity(r.severity());
        i.setIncidentStatus("OPEN");
        i.setInventoryCompleteness("COMPLETE");

        i.setRootComponent(
                components.findById(r.rootComponentId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Root component not found"))
        );

        i.setCreatedBy(
                users.findByEmail(email)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "User not found"))
        );

        return incidents.save(i);
    }

    // Automatically create an incident for a simulated failure.
    @Transactional
    public Incident createFromFailureEvent(
            Component component,
            String failureType,
            String systemUserEmail) {

        Incident incident = new Incident();

        incident.setIncidentCode(
                "INC-SIM-" +
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase()
        );

        incident.setTitle(
                "Network Failure: " + failureType
        );

        incident.setDescription(
                "Automatically generated incident for component "
                + component.getComponentCode()
                + ". Failure type: " + failureType
        );

        incident.setSeverity("HIGH");
        incident.setIncidentStatus("OPEN");
        incident.setInventoryCompleteness("COMPLETE");
        incident.setRootComponent(component);

        incident.setCreatedBy(
                users.findByEmail(systemUserEmail)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "System user not found: "
                                        + systemUserEmail))
        );

        return incidents.save(incident);
    }

    @Transactional
    public Incident status(Long id, String status) {

        Incident incident = get(id);
        incident.setIncidentStatus(status);

        return incidents.save(incident);
    }
}