package com.telecom.outage.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="alarms") @Getter @Setter @NoArgsConstructor
public class Alarm {

@Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="alarm_id") private Long id;

// EAGER so GET /api/alarms can serialize the component without a session.
@ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="component_id",nullable=false) private Component component;

@Column(name="alarm_code",nullable=false,length=50) private String alarmCode;
@Column(name="alarm_type",length=100) private String alarmType;
@Column(nullable=false,length=20) private String severity;
@Column(columnDefinition="text") private String description;
@Column(name="alarm_status",nullable=false,length=30) private String alarmStatus="OPEN";
@Column(name="occurred_at",nullable=false) private LocalDateTime occurredAt;
@Column(name="acknowledged_at") private LocalDateTime acknowledgedAt;
@Column(name="cleared_at") private LocalDateTime clearedAt;

@PrePersist void create(){if(occurredAt==null)occurredAt=LocalDateTime.now();}
}
