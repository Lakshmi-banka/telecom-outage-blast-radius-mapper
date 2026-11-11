package com.telecom.outage.repository;

import com.telecom.outage.entity.IncidentAffectedComponent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface IncidentAffectedComponentRepository
        extends JpaRepository<IncidentAffectedComponent, Long> {

    List<IncidentAffectedComponent> findByIncident_Id(Long incidentId);

    // ----------------------------------------------------------------------
    // Bulk delete, executed as a single SQL DELETE immediately.
    //
    // deleteAll(findByIncident_Id(id)) only MARKS entities for removal.
    // Hibernate runs deletes LAST at flush time, while save() on an
    // IDENTITY-generated entity runs its INSERT immediately (it needs the
    // generated key). The insert therefore hits rows that have not been
    // deleted yet and violates uq_incident_affected_component.
    //
    // flushAutomatically  -> pending changes are flushed before this runs
    // clearAutomatically  -> persistence context is cleared after, so no
    //                        stale deleted entities linger
    // ----------------------------------------------------------------------
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from IncidentAffectedComponent x where x.incident.id = :incidentId")
    void deleteByIncidentId(@Param("incidentId") Long incidentId);
}
