package com.telecom.outage.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.io.Serializable;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity @Table(name="incident_alarms") @IdClass(IncidentAlarm.Key.class) @Getter @Setter @NoArgsConstructor
public class IncidentAlarm {
@Id @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="incident_id") private Incident incident;
@Id @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="alarm_id") private Alarm alarm;
@Column(name="linked_at",nullable=false) private LocalDateTime linkedAt;
@PrePersist void create(){if(linkedAt==null)linkedAt=LocalDateTime.now();}
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public static class Key implements Serializable { private Long incident; private Long alarm; }
}