package com.telecom.outage.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.io.Serializable;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity @Table(name="component_dependencies",uniqueConstraints=@UniqueConstraint(name="uq_component_dependency",columnNames={"source_component_id","dependent_component_id"})) @Getter @Setter @NoArgsConstructor
public class ComponentDependency {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="dependency_id") private Long id;
@ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="source_component_id",nullable=false) private Component source;
@ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="dependent_component_id",nullable=false) private Component dependent;
@Column(name="dependency_type",nullable=false,length=50) private String dependencyType="REQUIRED";
@Column(name="is_active",nullable=false) private boolean active=true;
@Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
@PrePersist void create(){if(createdAt==null)createdAt=LocalDateTime.now();}
}