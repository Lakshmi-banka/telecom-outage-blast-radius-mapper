package com.telecom.outage.repository;
import com.telecom.outage.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AuditLogRepository extends JpaRepository<AuditLog,Long> {

}
