package com.telecom.outage.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.io.Serializable;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity @Table(name="incidents") @Getter @Setter @NoArgsConstructor
public class Incident {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="incident_id") private Long id;
@Column(name="incident_code",nullable=false,unique=true,length=50) private String incidentCode;
@Column(nullable=false,length=200) private String title;
@Column(columnDefinition="text") private String description;
@ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="root_component_id") private Component rootComponent;
@Column(nullable=false,length=20) private String severity;
@Column(name="incident_status",nullable=false,length=30) private String incidentStatus="OPEN";
@Column(name="started_at",nullable=false) private LocalDateTime startedAt;
@Column(name="resolved_at") private LocalDateTime resolvedAt;
@ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="created_by") private User createdBy;
@Column(name="inventory_completeness",nullable=false,length=30) private String inventoryCompleteness="UNKNOWN";
@Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
@Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
@PrePersist void create(){LocalDateTime n=LocalDateTime.now();if(startedAt==null)startedAt=n;createdAt=n;updatedAt=n;}
@PreUpdate void update(){updatedAt=LocalDateTime.now();}
}