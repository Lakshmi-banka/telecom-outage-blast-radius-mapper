package com.telecom.outage.controller;
import com.telecom.outage.dto.Requests.ComponentCreate;import com.telecom.outage.dto.Requests.StatusUpdate;import com.telecom.outage.service.ComponentService;import jakarta.validation.Valid;import org.springframework.security.access.prepost.PreAuthorize;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/components")
public class ComponentController {private final ComponentService s;public ComponentController(ComponentService s){this.s=s;}
@GetMapping public Object all(){return s.all();}@GetMapping("/{id}") public Object get(@PathVariable Long id){return s.get(id);}
@PostMapping @PreAuthorize("hasAnyRole('ADMIN','NETWORK_ENGINEER')") public Object create(@Valid @RequestBody ComponentCreate r){return s.create(r);}
@PatchMapping("/{id}/status") @PreAuthorize("hasAnyRole('ADMIN','NETWORK_ENGINEER')") public Object status(@PathVariable Long id,@Valid @RequestBody StatusUpdate r){return s.status(id,r.status());}}
