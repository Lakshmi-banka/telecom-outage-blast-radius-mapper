package com.telecom.outage.service;
import com.telecom.outage.dto.Requests.IncidentCreate;import com.telecom.outage.entity.Incident;
import com.telecom.outage.repository.*;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import java.util.*;
@Service
public class IncidentService {
 private final IncidentRepository incidents;private final ComponentRepository components;private final UserRepository users;
 public IncidentService(IncidentRepository i,ComponentRepository c,UserRepository u){incidents=i;components=c;users=u;}
 public List<Incident> all(){return incidents.findAll();}
 public Incident get(Long id){return incidents.findById(id).orElseThrow(()->new IllegalArgumentException("Incident not found"));}
 @Transactional public Incident create(IncidentCreate r,String email){if(incidents.findByIncidentCode(r.incidentCode()).isPresent())throw new IllegalArgumentException("Incident code exists");Incident i=new Incident();i.setIncidentCode(r.incidentCode());i.setTitle(r.title());i.setDescription(r.description());i.setSeverity(r.severity());i.setIncidentStatus("OPEN");i.setInventoryCompleteness("COMPLETE");i.setRootComponent(components.findById(r.rootComponentId()).orElseThrow(()->new IllegalArgumentException("Root component not found")));i.setCreatedBy(users.findByEmail(email).orElseThrow());return incidents.save(i);}
 @Transactional public Incident status(Long id,String status){Incident i=get(id);i.setIncidentStatus(status);return incidents.save(i);}
}
