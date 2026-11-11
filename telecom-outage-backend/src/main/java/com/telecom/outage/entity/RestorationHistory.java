package com.telecom.outage.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="restoration_history") @Getter @Setter @NoArgsConstructor
public class RestorationHistory {

@Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="restoration_id") private Long id;

// EAGER so GET /api/restorations serializes without a session.
@ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="incident_id",nullable=false) private Incident incident;
@ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="component_id") private Component component;

// @JsonIgnore: User carries passwordHash; keep it off the wire.
@JsonIgnore
@ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="performed_by") private User performedBy;

@Column(name="action_description",nullable=false,columnDefinition="text") private String actionDescription;
@Column(name="previous_status",length=30) private String previousStatus;
@Column(name="new_status",length=30) private String newStatus;
@Column(name="restoration_status",nullable=false,length=30) private String restorationStatus="IN_PROGRESS";
@Column(name="performed_at",nullable=false) private LocalDateTime performedAt;

// Added for engineer submission and independent admin verification.
@JsonIgnore
@ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="verified_by") private User verifiedBy;
@Column(name="verified_at") private LocalDateTime verifiedAt;
@Column(name="verification_notes", columnDefinition="text") private String verificationNotes;

@PrePersist void create(){if(performedAt==null)performedAt=LocalDateTime.now();}
}
