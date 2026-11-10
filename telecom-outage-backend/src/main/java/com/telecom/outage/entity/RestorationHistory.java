package com.telecom.outage.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.io.Serializable;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity @Table(name="restoration_history") @Getter @Setter @NoArgsConstructor
public class RestorationHistory {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="restoration_id") private Long id;
@ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="incident_id",nullable=false) private Incident incident;
@ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="component_id") private Component component;
@ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="performed_by") private User performedBy;
@Column(name="action_description",nullable=false,columnDefinition="text") private String actionDescription;
@Column(name="previous_status",length=30) private String previousStatus;
@Column(name="new_status",length=30) private String newStatus;
@Column(name="restoration_status",nullable=false,length=30) private String restorationStatus="IN_PROGRESS";
@Column(name="performed_at",nullable=false) private LocalDateTime performedAt;
@PrePersist void create(){if(performedAt==null)performedAt=LocalDateTime.now();}
}