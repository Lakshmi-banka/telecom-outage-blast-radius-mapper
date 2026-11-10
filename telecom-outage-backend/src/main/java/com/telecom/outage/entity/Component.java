package com.telecom.outage.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.io.Serializable;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity @Table(name="components") @Getter @Setter @NoArgsConstructor
public class Component {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="component_id") private Long id;
@Column(name="component_code",nullable=false,unique=true,length=50) private String componentCode;
@Column(name="component_name",nullable=false,length=150) private String componentName;
@Column(name="component_type",nullable=false,length=50) private String componentType;
@Column(length=200) private String location;
private BigDecimal latitude; private BigDecimal longitude;
@Column(nullable=false,length=30) private String status="UP";
@Column(nullable=false,length=20) private String criticality="MEDIUM";
@Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
@Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
@PrePersist void create(){LocalDateTime n=LocalDateTime.now();createdAt=n;updatedAt=n;}
@PreUpdate void update(){updatedAt=LocalDateTime.now();}
}