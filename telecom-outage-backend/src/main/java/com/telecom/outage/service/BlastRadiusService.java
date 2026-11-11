
package com.telecom.outage.service;

import com.telecom.outage.dto.PythonEngineDtos;
import com.telecom.outage.entity.*;
import com.telecom.outage.integration.PythonEngineClient;
import com.telecom.outage.integration.PythonTopologyService;
import com.telecom.outage.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class BlastRadiusService {

    private final PythonEngineClient pythonEngineClient;
    private final PythonTopologyService pythonTopologyService;
    private final IncidentRepository incidentRepository;
    private final ComponentRepository componentRepository;
    private final ServiceEntityRepository serviceRepository;
    private final IncidentAffectedComponentRepository affectedComponentRepository;
    private final IncidentAffectedServiceRepository affectedServiceRepository;

    public BlastRadiusService(
            PythonEngineClient pythonEngineClient,
            PythonTopologyService pythonTopologyService,
            IncidentRepository incidentRepository,
            ComponentRepository componentRepository,
            ServiceEntityRepository serviceRepository,
            IncidentAffectedComponentRepository affectedComponentRepository,
            IncidentAffectedServiceRepository affectedServiceRepository) {

        this.pythonEngineClient = pythonEngineClient;
        this.pythonTopologyService = pythonTopologyService;
        this.incidentRepository = incidentRepository;
        this.componentRepository = componentRepository;
        this.serviceRepository = serviceRepository;
        this.affectedComponentRepository = affectedComponentRepository;
        this.affectedServiceRepository = affectedServiceRepository;
    }

    // ==========================================
    // 1. BFS BLAST RADIUS ANALYSIS
    // ==========================================

    @Transactional
    public Map<String, Object> analyze(Long incidentId) {

        Incident incident = getIncident(incidentId);

        PythonEngineDtos.TopologyRequest topology =
                pythonTopologyService.buildTopology(incidentId);

        Map<?, ?> pythonResult =
                pythonEngineClient.calculateBfs(topology);

        return persistBlastRadiusResult(incident, pythonResult);
    }

    // ==========================================
    // 2. DFS BLAST RADIUS ANALYSIS
    // ==========================================

    @Transactional
    public Map<String, Object> analyzeDfs(Long incidentId) {

        Incident incident = getIncident(incidentId);

        PythonEngineDtos.TopologyRequest topology =
                pythonTopologyService.buildTopology(incidentId);

        Map<?, ?> pythonResult =
                pythonEngineClient.calculateDfs(topology);

        return persistBlastRadiusResult(incident, pythonResult);
    }

    // ==========================================
    // 3. GET INCIDENT
    // ==========================================

    private Incident getIncident(Long incidentId) {

        return incidentRepository.findById(incidentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Incident not found: " + incidentId));
    }

    // ==========================================
    // 4. COMMON DATABASE PERSISTENCE
    // ==========================================

    private Map<String, Object> persistBlastRadiusResult(
            Incident incident,
            Map<?, ?> pythonResult) {

        if (pythonResult == null) {
            throw new IllegalStateException(
                    "Python engine returned an empty response");
        }

        Long incidentId = incident.getId();

        Component rootComponent = incident.getRootComponent();

        if (rootComponent == null) {
            throw new IllegalArgumentException(
                    "Incident does not have a root component");
        }

        // Prepare component lookup
        Map<String, Component> componentMap = new HashMap<>();

        for (Component component : componentRepository.findAll()) {
            componentMap.put(
                    component.getComponentCode(),
                    component
            );
        }

        // Prepare service lookup
        Map<String, ServiceEntity> serviceMap = new HashMap<>();

        for (ServiceEntity service : serviceRepository.findAll()) {
            serviceMap.put(
                    service.getServiceCode(),
                    service
            );
        }

        // Extract Python response data
        List<?> affectedComponents =
                getList(pythonResult, "affected_components");

        List<?> affectedServices =
                getList(pythonResult, "affected_services");

        // ==========================================
        // CLEAR PREVIOUS BLAST-RADIUS RESULTS
        //
        // Must be a bulk delete, NOT deleteAll(findBy...).
        //
        // deleteAll() only marks entities for removal; Hibernate executes
        // deletes LAST at flush time. But these entities use
        // GenerationType.IDENTITY, so save() below issues its INSERT
        // IMMEDIATELY to obtain the generated key. The insert then collides
        // with rows that have not been deleted yet:
        //
        //   ERROR: duplicate key value violates unique constraint
        //          "uq_incident_affected_component"
        //   Detail: Key (incident_id, component_id)=(1, 1) already exists.
        //
        // The @Modifying bulk delete runs a real DELETE statement right now.
        // ==========================================

        affectedComponentRepository.deleteByIncidentId(incidentId);
        affectedServiceRepository.deleteByIncidentId(incidentId);

        // ==========================================
        // 5. SAVE ROOT COMPONENT
        // ==========================================

        IncidentAffectedComponent rootAffected =
                new IncidentAffectedComponent();

        rootAffected.setIncident(incident);
        rootAffected.setComponent(rootComponent);
        rootAffected.setRootCause(true);
        rootAffected.setImpactStatus("CONFIRMED");
        rootAffected.setImpactLevel(rootComponent.getCriticality());

        affectedComponentRepository.save(rootAffected);

        List<Map<String, Object>> componentOutput =
                new ArrayList<>();

        componentOutput.add(Map.of(
                "componentCode", rootComponent.getComponentCode(),
                "componentName", rootComponent.getComponentName(),
                "criticality", rootComponent.getCriticality(),
                "rootCause", true
        ));

        // ==========================================
        // 6. SAVE AFFECTED COMPONENTS
        // ==========================================

        Set<String> savedComponentCodes = new HashSet<>();

        savedComponentCodes.add(rootComponent.getComponentCode());

        for (Object item : affectedComponents) {

            if (!(item instanceof Map<?, ?> componentData)) {
                throw new IllegalStateException(
                        "Invalid affected component returned by Python");
            }

            String componentCode =
                    String.valueOf(componentData.get("component_id"));

            // Avoid duplicate component records
            if (!savedComponentCodes.add(componentCode)) {
                continue;
            }

            Component component = componentMap.get(componentCode);

            if (component == null) {
                throw new IllegalStateException(
                        "Component not found in database: " + componentCode);
            }

            IncidentAffectedComponent affected =
                    new IncidentAffectedComponent();

            affected.setIncident(incident);
            affected.setComponent(component);
            affected.setRootCause(false);
            affected.setImpactStatus("ESTIMATED");
            affected.setImpactLevel(component.getCriticality());

            affectedComponentRepository.save(affected);

            componentOutput.add(Map.of(
                    "componentCode", component.getComponentCode(),
                    "componentName", component.getComponentName(),
                    "criticality", component.getCriticality(),
                    "rootCause", false
            ));
        }

        // ==========================================
        // 7. SAVE AFFECTED SERVICES
        // ==========================================

        List<Map<String, Object>> serviceOutput =
                new ArrayList<>();

        Set<String> savedServiceCodes = new HashSet<>();

        for (Object item : affectedServices) {

            if (!(item instanceof Map<?, ?> serviceData)) {
                throw new IllegalStateException(
                        "Invalid affected service returned by Python");
            }

            String serviceCode =
                    String.valueOf(serviceData.get("service_id"));

            // Avoid duplicate service records
            if (!savedServiceCodes.add(serviceCode)) {
                continue;
            }

            ServiceEntity service = serviceMap.get(serviceCode);

            if (service == null) {
                throw new IllegalStateException(
                        "Service not found in database: " + serviceCode);
            }

            Object customerCountValue =
                    serviceData.get("estimated_customer_count");

            if (!(customerCountValue instanceof Number customerCount)) {
                throw new IllegalStateException(
                        "Invalid customer count for service: " + serviceCode);
            }

            IncidentAffectedService affected =
                    new IncidentAffectedService();

            affected.setIncident(incident);
            affected.setService(service);
            affected.setImpactStatus("ESTIMATED");

            affected.setEstimatedAffectedCustomers(
                    customerCount.longValue()
            );

            affectedServiceRepository.save(affected);

            serviceOutput.add(Map.of(
                    "serviceCode", service.getServiceCode(),
                    "serviceName", service.getServiceName(),
                    "estimatedCustomers", customerCount.longValue()
            ));
        }

        // ==========================================
        // 8. BUILD FINAL RESPONSE
        // ==========================================

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("incidentId", incidentId);
        response.put("rootComponent", rootComponent.getComponentCode());
        response.put("traversal", pythonResult.get("traversal"));

        response.put(
                "affectedComponentCount",
                componentOutput.size()
        );

        response.put(
                "affectedServiceCount",
                serviceOutput.size()
        );

        response.put(
                "estimatedAffectedCustomers",
                pythonResult.get("estimated_affected_customers")
        );

        response.put("components", componentOutput);
        response.put("services", serviceOutput);

        response.put(
                "visitedOrder",
                pythonResult.get("visited_order")
        );

        return response;
    }

    // ==========================================
    // 9. SAFE LIST EXTRACTION
    // ==========================================

    private List<?> getList(Map<?, ?> result, String key) {

        Object value = result.get(key);

        if (value == null) {
            return Collections.emptyList();
        }

        if (!(value instanceof List<?> list)) {
            throw new IllegalStateException(
                    "Invalid Python response: " + key + " must be a list");
        }

        return list;
    }
}
