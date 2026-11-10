package com.telecom.outage.repository;
import com.telecom.outage.entity.IncidentAlarm;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface IncidentAlarmRepository extends JpaRepository<IncidentAlarm,IncidentAlarm.Key> {

}
