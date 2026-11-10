package com.telecom.outage.service;
import com.telecom.outage.dto.Requests.ComponentCreate;
import com.telecom.outage.entity.Component;
import com.telecom.outage.repository.ComponentRepository;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import java.util.*;
@Service
public class ComponentService {
 private final ComponentRepository repo;public ComponentService(ComponentRepository r){repo=r;}
 public List<Component> all(){return repo.findAll();}
 public Component get(Long id){return repo.findById(id).orElseThrow(()->new IllegalArgumentException("Component not found"));}
 @Transactional public Component create(ComponentCreate r){if(repo.findByComponentCode(r.componentCode()).isPresent())throw new IllegalArgumentException("Component code exists");Component c=new Component();c.setComponentCode(r.componentCode());c.setComponentName(r.componentName());c.setComponentType(r.componentType());c.setLocation(r.location());c.setLatitude(r.latitude());c.setLongitude(r.longitude());c.setStatus(r.status());c.setCriticality(r.criticality());return repo.save(c);}
 @Transactional public Component status(Long id,String status){Component c=get(id);c.setStatus(status);return repo.save(c);}
}
