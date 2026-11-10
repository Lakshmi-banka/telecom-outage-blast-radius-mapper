package com.telecom.outage.repository;
import com.telecom.outage.entity.IncidentAffectedService;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface IncidentAffectedServiceRepository extends JpaRepository<IncidentAffectedService,Long> {
 List<IncidentAffectedService> findByIncident_Id(Long incidentId);
}
