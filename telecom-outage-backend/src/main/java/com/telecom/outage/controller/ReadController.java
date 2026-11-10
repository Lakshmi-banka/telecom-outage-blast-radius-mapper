package com.telecom.outage.controller;
import com.telecom.outage.repository.*;import org.springframework.web.bind.annotation.*;
@RestController
public class ReadController {
 private final AlarmRepository alarms;private final ServiceEntityRepository services;private final ComponentDependencyRepository deps;
 public ReadController(AlarmRepository a,ServiceEntityRepository s,ComponentDependencyRepository d){alarms=a;services=s;deps=d;}
 @GetMapping("/api/alarms") public Object alarms(){return alarms.findAll();}
 @GetMapping("/api/alarms/status/{status}") public Object alarms(@PathVariable String status){return alarms.findByAlarmStatus(status.toUpperCase());}
 @GetMapping("/api/services") public Object services(){return services.findAll();}
 @GetMapping("/api/network/dependencies") public Object dependencies(){return deps.findByActiveTrue();}
}
