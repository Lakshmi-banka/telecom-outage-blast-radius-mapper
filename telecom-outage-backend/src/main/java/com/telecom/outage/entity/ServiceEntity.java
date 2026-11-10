package com.telecom.outage.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.io.Serializable;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity @Table(name="services") @Getter @Setter @NoArgsConstructor
public class ServiceEntity {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="service_id") private Long id;
@Column(name="service_code",nullable=false,unique=true,length=50) private String serviceCode;
@Column(name="service_name",nullable=false,length=150) private String serviceName;
@Column(name="service_type",length=50) private String serviceType;
@Column(name="estimated_customer_count",nullable=false) private Long estimatedCustomerCount=0L;
@Column(nullable=false,length=30) private String status="ACTIVE";
@Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
@Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
@PrePersist void create(){LocalDateTime n=LocalDateTime.now();createdAt=n;updatedAt=n;}
@PreUpdate void update(){updatedAt=LocalDateTime.now();}
}