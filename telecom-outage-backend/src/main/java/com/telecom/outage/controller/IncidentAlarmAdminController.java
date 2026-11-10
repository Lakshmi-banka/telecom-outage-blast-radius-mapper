package com.telecom.outage.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/admin/incident-alarms")
@PreAuthorize("hasRole('ADMIN')")
public class IncidentAlarmAdminController {
    private final JdbcTemplate jdbc;
    public IncidentAlarmAdminController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping
    public List<Map<String,Object>> all() {
        return jdbc.queryForList("SELECT incident_id, alarm_id, linked_at FROM incident_alarms ORDER BY linked_at DESC");
    }

    @PostMapping
    public Map<String,Object> link(@RequestBody Map<String,Long> body) {
        Long incidentId = body.get("incidentId"), alarmId = body.get("alarmId");
        if (incidentId == null || alarmId == null) throw new IllegalArgumentException("incidentId and alarmId are required");
        jdbc.update("INSERT INTO incident_alarms (incident_id, alarm_id) VALUES (?, ?)", incidentId, alarmId);
        return Map.of("incidentId", incidentId, "alarmId", alarmId, "linked", true);
    }

    @DeleteMapping("/{incidentId}/{alarmId}")
    public Map<String,Object> unlink(@PathVariable Long incidentId, @PathVariable Long alarmId) {
        int count = jdbc.update("DELETE FROM incident_alarms WHERE incident_id=? AND alarm_id=?", incidentId, alarmId);
        if (count == 0) throw new NoSuchElementException("Incident-alarm link not found");
        return Map.of("deleted", true, "incidentId", incidentId, "alarmId", alarmId);
    }
}
