package com.telecom.outage.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.io.Serializable;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity @Table(name="service_components",uniqueConstraints=@UniqueConstraint(name="uq_service_component",columnNames={"service_id","component_id"})) @Getter @Setter @NoArgsConstructor
public class ServiceComponent {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="service_component_id") private Long id;
@ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="service_id",nullable=false) private ServiceEntity service;
@ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="component_id",nullable=false) private Component component;
@Column(name="is_critical",nullable=false) private boolean critical=true;
@Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
@PrePersist void create(){if(createdAt==null)createdAt=LocalDateTime.now();}
}