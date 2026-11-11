package com.telecom.outage.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "failure_events")
@Getter @Setter @NoArgsConstructor
public class FailureEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "failure_event_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "failed_component_id", nullable = false)
    private Component failedComponent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alarm_id")
    private Alarm alarm;

    @Column(name = "event_source", nullable = false)
    private String eventSource;
    @Column(name = "failure_type")
    private String failureType;
    @Column(name = "event_status", nullable = false)
    private String eventStatus = "ACTIVE";
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;
    @Column(name = "scheduled_recovery_at")
    private LocalDateTime scheduledRecoveryAt;
    @Column(name = "recovered_at")
    private LocalDateTime recoveredAt;

    @PrePersist
    void beforeInsert() { if (startedAt == null) startedAt = LocalDateTime.now(); }
}
