package com.telecom.outage.service;
import com.telecom.outage.entity.*;import com.telecom.outage.repository.*;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import java.util.*;
@Service
public class BlastRadiusService {
 private final ComponentDependencyRepository deps;private final ComponentRepository components;private final ServiceComponentRepository mappings;private final ServiceEntityRepository services;private final IncidentRepository incidents;private final IncidentAffectedComponentRepository affectedComponents;private final IncidentAffectedServiceRepository affectedServices;
 public BlastRadiusService(ComponentDependencyRepository d,ComponentRepository c,ServiceComponentRepository m,ServiceEntityRepository s,IncidentRepository i,IncidentAffectedComponentRepository ac,IncidentAffectedServiceRepository as){deps=d;components=c;mappings=m;services=s;incidents=i;affectedComponents=ac;affectedServices=as;}
 @Transactional public Map<String,Object> analyze(Long id){
  Incident incident=incidents.findById(id).orElseThrow(()->new IllegalArgumentException("Incident not found"));if(incident.getRootComponent()==null)throw new IllegalArgumentException("Incident has no root component");
  Map<Long,List<Long>> graph=new HashMap<>();for(var e:deps.findByActiveTrue())graph.computeIfAbsent(e.getSource().getId(),k->new ArrayList<>()).add(e.getDependent().getId());
  Set<Long> seen=new LinkedHashSet<>();Deque<Long> q=new ArrayDeque<>();q.add(incident.getRootComponent().getId());while(!q.isEmpty()){Long n=q.remove();if(seen.add(n))q.addAll(graph.getOrDefault(n,List.of()));}
  affectedComponents.deleteAll(affectedComponents.findByIncident_Id(id));List<Component> impacted=components.findAllById(seen);
  for(Component c:impacted){IncidentAffectedComponent a=new IncidentAffectedComponent();a.setIncident(incident);a.setComponent(c);boolean root=c.getId().equals(incident.getRootComponent().getId());a.setRootCause(root);a.setImpactStatus(root?"CONFIRMED":"ESTIMATED");a.setImpactLevel(root?"CRITICAL":c.getCriticality());affectedComponents.save(a);}
  Set<Long> serviceIds=new HashSet<>();for(ServiceComponent sc:mappings.findAll())if(seen.contains(sc.getComponent().getId()))serviceIds.add(sc.getService().getId());
  affectedServices.deleteAll(affectedServices.findByIncident_Id(id));List<Map<String,Object>> serviceOut=new ArrayList<>();
  for(ServiceEntity s:services.findAllById(serviceIds)){IncidentAffectedService a=new IncidentAffectedService();a.setIncident(incident);a.setService(s);a.setImpactStatus("ESTIMATED");a.setEstimatedAffectedCustomers(s.getEstimatedCustomerCount());affectedServices.save(a);serviceOut.add(Map.of("serviceCode",s.getServiceCode(),"serviceName",s.getServiceName(),"estimatedCustomers",s.getEstimatedCustomerCount()));}
  List<Map<String,Object>> compOut=impacted.stream().map(c->Map.<String,Object>of("componentCode",c.getComponentCode(),"componentName",c.getComponentName(),"criticality",c.getCriticality(),"rootCause",c.getId().equals(incident.getRootComponent().getId()))).toList();
  return Map.of("incidentId",id,"rootComponent",incident.getRootComponent().getComponentCode(),"affectedComponentCount",compOut.size(),"affectedServiceCount",serviceOut.size(),"components",compOut,"services",serviceOut);
 }
}
