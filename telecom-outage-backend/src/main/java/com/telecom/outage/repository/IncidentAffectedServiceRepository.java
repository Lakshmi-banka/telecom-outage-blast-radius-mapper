package com.telecom.outage.repository;

import com.telecom.outage.entity.IncidentAffectedService;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface IncidentAffectedServiceRepository
        extends JpaRepository<IncidentAffectedService, Long> {

    List<IncidentAffectedService> findByIncident_Id(Long incidentId);

    // Same reasoning as IncidentAffectedComponentRepository.deleteByIncidentId
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from IncidentAffectedService x where x.incident.id = :incidentId")
    void deleteByIncidentId(@Param("incidentId") Long incidentId);
}
