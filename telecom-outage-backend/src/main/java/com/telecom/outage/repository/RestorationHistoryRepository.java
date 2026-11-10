package com.telecom.outage.repository;
import com.telecom.outage.entity.RestorationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface RestorationHistoryRepository extends JpaRepository<RestorationHistory,Long> {
 List<RestorationHistory> findByIncident_IdOrderByPerformedAtDesc(Long incidentId);
}
