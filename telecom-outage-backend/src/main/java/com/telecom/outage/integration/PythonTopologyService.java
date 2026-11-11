package com.telecom.outage.integration;

import com.telecom.outage.dto.PythonEngineDtos;
import com.telecom.outage.entity.*;
import com.telecom.outage.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PythonTopologyService {

    private final ComponentRepository componentRepository;
    private final ComponentDependencyRepository dependencyRepository;
    private final ServiceEntityRepository serviceRepository;
    private final ServiceComponentRepository serviceComponentRepository;
    private final IncidentRepository incidentRepository;

    public PythonTopologyService(
            ComponentRepository componentRepository,
            ComponentDependencyRepository dependencyRepository,
            ServiceEntityRepository serviceRepository,
            ServiceComponentRepository serviceComponentRepository,
            IncidentRepository incidentRepository) {

        this.componentRepository = componentRepository;
        this.dependencyRepository = dependencyRepository;
        this.serviceRepository = serviceRepository;
        this.serviceComponentRepository = serviceComponentRepository;
        this.incidentRepository = incidentRepository;
    }

    @Transactional(readOnly = true)
    public PythonEngineDtos.TopologyRequest buildTopology(Long incidentId) {

        // 1. Get incident and its root component
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Incident not found: " + incidentId));

        if (incident.getRootComponent() == null) {
            throw new IllegalArgumentException(
                    "Incident does not have a root component");
        }

        String rootComponentCode =
                incident.getRootComponent().getComponentCode();

        // 2. Fetch all components
        List<PythonEngineDtos.ComponentDTO> components =
                componentRepository.findAll()
                        .stream()
                        .map(component ->
                                new PythonEngineDtos.ComponentDTO(
                                        component.getComponentCode(),
                                        component.getComponentName(),
                                        component.getStatus(),
                                        component.getCriticality()
                                ))
                        .toList();

        // 3. Fetch active dependencies
        List<PythonEngineDtos.DependencyDTO> dependencies =
                dependencyRepository.findByActiveTrue()
                        .stream()
                        .map(dependency ->
                                new PythonEngineDtos.DependencyDTO(
                                        dependency.getSource()
                                                .getComponentCode(),
                                        dependency.getDependent()
                                                .getComponentCode(),
                                        dependency.isActive()
                                ))
                        .toList();

        // 4. Fetch service-component mappings
        List<ServiceComponent> mappings =
                serviceComponentRepository.findAll();

        Map<Long, List<String>> serviceComponentMap =
                mappings.stream()
                        .collect(Collectors.groupingBy(
                                mapping -> mapping.getService().getId(),
                                Collectors.mapping(
                                        mapping -> mapping.getComponent()
                                                .getComponentCode(),
                                        Collectors.toList()
                                )
                        ));

        // 5. Fetch services and their associated components
        List<PythonEngineDtos.ServiceDTO> services =
                serviceRepository.findAll()
                        .stream()
                        .map(service ->
                                new PythonEngineDtos.ServiceDTO(
                                        service.getServiceCode(),
                                        service.getServiceName(),
                                        service.getEstimatedCustomerCount(),
                                        serviceComponentMap.getOrDefault(
                                                service.getId(),
                                                Collections.emptyList()
                                        ),
                                        service.getStatus()
                                ))
                        .toList();
        if (components.isEmpty()) {
    throw new IllegalStateException(
            "No components found in PostgreSQL");
}

if (dependencies.isEmpty()) {
    System.out.println("WARNING: No active dependencies found");
}

if (services.isEmpty()) {
    System.out.println("WARNING: No services found");
}

System.out.println("Topology component count: " + components.size());
System.out.println("Topology dependency count: " + dependencies.size());
System.out.println("Topology service count: " + services.size());
System.out.println("Root component: " + rootComponentCode);

        // 6. Build the final topology request
        return new PythonEngineDtos.TopologyRequest(
                rootComponentCode,
                components,
                dependencies,
                services
        );
    }
}
