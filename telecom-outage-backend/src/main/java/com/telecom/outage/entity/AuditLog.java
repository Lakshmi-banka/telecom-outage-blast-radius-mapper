package com.telecom.outage.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.io.Serializable;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity @Table(name="audit_logs") @Getter @Setter @NoArgsConstructor
public class AuditLog {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="audit_id") private Long id;
@ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id") private User user;
@Column(nullable=false,length=100) private String action;
@Column(name="entity_type",length=100) private String entityType;
@Column(name="entity_id") private Long entityId;
@JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb") private Map<String,Object> details;
@Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
@PrePersist void create(){if(createdAt==null)createdAt=LocalDateTime.now();}
}