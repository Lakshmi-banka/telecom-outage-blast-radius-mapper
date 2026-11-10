package com.telecom.outage.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.io.Serializable;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity @Table(name="incident_affected_services",uniqueConstraints=@UniqueConstraint(name="uq_incident_affected_service",columnNames={"incident_id","service_id"})) @Getter @Setter @NoArgsConstructor
public class IncidentAffectedService {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="incident_affected_service_id") private Long id;
@ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="incident_id",nullable=false) private Incident incident;
@ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="service_id",nullable=false) private ServiceEntity service;
@Column(name="impact_status",nullable=false,length=30) private String impactStatus="ESTIMATED";
@Column(name="estimated_affected_customers",nullable=false) private Long estimatedAffectedCustomers=0L;
@Column(name="detected_at",nullable=false) private LocalDateTime detectedAt;
@PrePersist void create(){if(detectedAt==null)detectedAt=LocalDateTime.now();}
}