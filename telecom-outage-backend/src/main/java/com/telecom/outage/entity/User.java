package com.telecom.outage.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="users") @Getter @Setter @NoArgsConstructor
public class User {

@Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="user_id") private Long id;
@Column(name="full_name",nullable=false,length=100) private String fullName;
@Column(nullable=false,unique=true,length=150) private String email;

// @JsonIgnore so a bcrypt hash can never leak through any endpoint that
// happens to serialize a User.
@JsonIgnore
@Column(name="password_hash",nullable=false) private String passwordHash;

@ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="role_id",nullable=false) private Role role;
@Column(name="is_active",nullable=false) private boolean active=true;
@Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
@Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;

@PrePersist void create(){LocalDateTime n=LocalDateTime.now();createdAt=n;updatedAt=n;}
@PreUpdate void update(){updatedAt=LocalDateTime.now();}
}
