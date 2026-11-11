package com.telecom.outage.controller;
import com.telecom.outage.dto.Requests.*;import com.telecom.outage.service.*;import jakarta.validation.Valid;import org.springframework.security.access.prepost.PreAuthorize;import org.springframework.security.core.Authentication;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/incidents")
public class IncidentController {private final IncidentService s;private final BlastRadiusService blast;public IncidentController(IncidentService s,BlastRadiusService b){this.s=s;blast=b;}
@GetMapping public Object all(){return s.all();}@GetMapping("/{id}") public Object get(@PathVariable Long id){return s.get(id);}
@PostMapping @PreAuthorize("hasAnyRole('ADMIN','NOC_MANAGER','NOC_ANALYST')") public Object create(@Valid @RequestBody IncidentCreate r,Authentication a){return s.create(r,a.getName());}
@PatchMapping("/{id}/status") @PreAuthorize("hasAnyRole('ADMIN','NOC_MANAGER')") public Object status(@PathVariable Long id,@Valid @RequestBody IncidentStatusUpdate r){return s.status(id,r.incidentStatus());}
@PostMapping("/{id}/blast-radius") @PreAuthorize("hasAnyRole('ADMIN','NOC_MANAGER','NOC_ANALYST')") public Object blast(@PathVariable Long id){return blast.analyze(id);}
@PostMapping("/{id}/blast-radius/dfs")
@PreAuthorize("hasAnyRole('ADMIN','NOC_MANAGER','NOC_ANALYST')")
public Object analyzeDfs(@PathVariable Long id) {
    return blast.analyzeDfs(id);
}
}
