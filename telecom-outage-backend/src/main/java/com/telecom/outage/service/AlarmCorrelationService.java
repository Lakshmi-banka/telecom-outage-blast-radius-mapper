
package com.telecom.outage.service;

import com.telecom.outage.dto.PythonEngineDtos;
import com.telecom.outage.entity.Alarm;
import com.telecom.outage.integration.PythonEngineClient;
import com.telecom.outage.repository.AlarmRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class AlarmCorrelationService {

    private final AlarmRepository alarmRepository;
    private final PythonEngineClient pythonEngineClient;

    public AlarmCorrelationService(
            AlarmRepository alarmRepository,
            PythonEngineClient pythonEngineClient) {

        this.alarmRepository = alarmRepository;
        this.pythonEngineClient = pythonEngineClient;
    }

    @Transactional(readOnly = true)
    public Map<?, ?> correlateAlarms(
            int windowMinutes, String groupBy) {

        if (windowMinutes < 1 || windowMinutes > 1440) {
            throw new IllegalArgumentException(
                    "Window must be between 1 and 1440 minutes");
        }

        if (!"component".equals(groupBy)
                && !"component_and_type".equals(groupBy)) {
            throw new IllegalArgumentException(
                    "groupBy must be component or component_and_type");
        }

        // Fetch alarms from PostgreSQL
        List<Alarm> alarms = alarmRepository.findAll();

        // Convert database alarms to Python DTOs
        List<PythonEngineDtos.AlarmDTO> pythonAlarms =
                alarms.stream()
                        .map(alarm -> new PythonEngineDtos.AlarmDTO(
                                String.valueOf(alarm.getId()),
                                alarm.getComponent().getComponentCode(),
                                alarm.getAlarmCode(),
                                alarm.getAlarmType() == null
                                        ? "UNKNOWN"
                                        : alarm.getAlarmType(),
                                alarm.getSeverity(),
                                alarm.getDescription() == null
                                        ? ""
                                        : alarm.getDescription(),
                                alarm.getAlarmStatus(),
                                alarm.getOccurredAt()
                        ))
                        .toList();

        // Build Python request
        PythonEngineDtos.AlarmCorrelationRequest request =
                new PythonEngineDtos.AlarmCorrelationRequest(
                        pythonAlarms,
                        windowMinutes,
                        groupBy
                );

        // Send request to Python
        return pythonEngineClient.correlateAlarms(request);
    }
}
