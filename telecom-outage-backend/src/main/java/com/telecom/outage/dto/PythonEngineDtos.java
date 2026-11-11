package com.telecom.outage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public class PythonEngineDtos {

    // Component information
    public record ComponentDTO(
            @JsonProperty("component_id") String componentId,
            String name,
            String status,
            String criticality
    ) {}

    // Dependency between two components
    public record DependencyDTO(
            @JsonProperty("source_component_id") String sourceComponentId,
            @JsonProperty("dependent_component_id") String dependentComponentId,
            @JsonProperty("is_active") boolean active
    ) {}

    // Service information
    public record ServiceDTO(
            @JsonProperty("service_id") String serviceId,
            @JsonProperty("service_name") String serviceName,
            @JsonProperty("estimated_customer_count") long estimatedCustomerCount,
            @JsonProperty("component_ids") List<String> componentIds,
            String status
    ) {}

    // Complete network topology
    public record TopologyRequest(
            @JsonProperty("root_component_id") String rootComponentId,
            List<ComponentDTO> components,
            List<DependencyDTO> dependencies,
            List<ServiceDTO> services
    ) {}

    // Alarm information
    public record AlarmDTO(
            @JsonProperty("alarm_id") String alarmId,
            @JsonProperty("component_id") String componentId,
            @JsonProperty("alarm_code") String alarmCode,
            @JsonProperty("alarm_type") String alarmType,
            String severity,
            String description,
            @JsonProperty("alarm_status") String alarmStatus,
            @JsonProperty("occurred_at") LocalDateTime occurredAt
    ) {}

    // Alarm correlation request
    public record AlarmCorrelationRequest(
            List<AlarmDTO> alarms,
            @JsonProperty("window_minutes") int windowMinutes,
            @JsonProperty("group_by") String groupBy
    ) {}

    // Outage simulation request
    public record SimulationRequest(
            @JsonProperty("root_component_id") String rootComponentId,
            List<ComponentDTO> components,
            List<DependencyDTO> dependencies,
            List<ServiceDTO> services,
            @JsonProperty("failed_component_ids") List<String> failedComponentIds,
            @JsonProperty("include_root_in_affected") boolean includeRootInAffected,
            String traversal
    ) {}
}
