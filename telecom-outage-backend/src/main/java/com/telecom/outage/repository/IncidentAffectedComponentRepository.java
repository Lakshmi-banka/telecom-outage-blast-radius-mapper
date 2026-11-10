package com.telecom.outage.repository;
import com.telecom.outage.entity.IncidentAffectedComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface IncidentAffectedComponentRepository extends JpaRepository<IncidentAffectedComponent,Long> {
 List<IncidentAffectedComponent> findByIncident_Id(Long incidentId);
}
