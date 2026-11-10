package com.telecom.outage.repository;
import com.telecom.outage.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface IncidentRepository extends JpaRepository<Incident,Long> {
 Optional<Incident> findByIncidentCode(String incidentCode); List<Incident> findByIncidentStatus(String status);
}
