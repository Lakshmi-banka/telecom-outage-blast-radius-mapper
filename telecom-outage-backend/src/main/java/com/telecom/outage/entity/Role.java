package com.telecom.outage.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.io.Serializable;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity @Table(name="roles") @Getter @Setter @NoArgsConstructor
public class Role {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="role_id") private Long id;
@Column(name="role_name",nullable=false,unique=true,length=50) private String roleName;
private String description;
@Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
@PrePersist void create(){if(createdAt==null)createdAt=LocalDateTime.now();}
}