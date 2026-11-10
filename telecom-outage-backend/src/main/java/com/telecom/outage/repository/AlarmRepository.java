package com.telecom.outage.repository;
import com.telecom.outage.entity.Alarm;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AlarmRepository extends JpaRepository<Alarm,Long> {
 List<Alarm> findByAlarmStatus(String alarmStatus);
}
