package com.telecom.outage.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="incidents") @Getter @Setter @NoArgsConstructor
public class Incident {

@Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="incident_id") private Long id;
@Column(name="incident_code",nullable=false,unique=true,length=50) private String incidentCode;
@Column(nullable=false,length=200) private String title;
@Column(columnDefinition="text") private String description;

// EAGER: spring.jpa.open-in-view=false means the Hibernate session is already
// closed when Jackson serializes this entity. A LAZY proxy here throws
// "Could not initialize proxy - no session" and GET /api/incidents returns 500.
@ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="root_component_id") private Component rootComponent;

@Column(nullable=false,length=20) private String severity;
@Column(name="incident_status",nullable=false,length=30) private String incidentStatus="OPEN";
@Column(name="started_at",nullable=false) private LocalDateTime startedAt;
@Column(name="resolved_at") private LocalDateTime resolvedAt;

// @JsonIgnore: never serialize the User entity to the client -- it carries
// passwordHash. It is still written and read normally by JPA.
@JsonIgnore
@ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="created_by") private User createdBy;

@Column(name="inventory_completeness",nullable=false,length=30) private String inventoryCompleteness="UNKNOWN";
@Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
@Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;

@PrePersist void create(){LocalDateTime n=LocalDateTime.now();if(startedAt==null)startedAt=n;createdAt=n;updatedAt=n;}
@PreUpdate void update(){updatedAt=LocalDateTime.now();}
}
