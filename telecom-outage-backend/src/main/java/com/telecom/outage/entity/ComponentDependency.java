package com.telecom.outage.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name="component_dependencies",
       uniqueConstraints=@UniqueConstraint(name="uq_component_dependency",
               columnNames={"source_component_id","dependent_component_id"}))
@Getter @Setter @NoArgsConstructor
public class ComponentDependency {

@Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="dependency_id") private Long id;

// EAGER on both sides so GET /api/network/dependencies can serialize the
// component codes the frontend draws the topology graph from.
@ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="source_component_id",nullable=false) private Component source;
@ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="dependent_component_id",nullable=false) private Component dependent;

@Column(name="dependency_type",nullable=false,length=50) private String dependencyType="REQUIRED";
@Column(name="is_active",nullable=false) private boolean active=true;
@Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;

@PrePersist void create(){if(createdAt==null)createdAt=LocalDateTime.now();}
}
