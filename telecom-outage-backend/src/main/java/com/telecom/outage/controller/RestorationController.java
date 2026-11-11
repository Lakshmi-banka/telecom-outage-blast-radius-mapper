package com.telecom.outage.controller;

import com.telecom.outage.dto.Requests.RestorationCreate;
import com.telecom.outage.dto.Requests.RestorationVerification;
import com.telecom.outage.service.RestorationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/restorations")
public class RestorationController {
    private final RestorationService service;
    public RestorationController(RestorationService service) { this.service = service; }

    @GetMapping
    public Object all() { return service.all(); }

    @GetMapping("/{id}")
    public Object get(@PathVariable Long id) { return service.get(id); }

    @GetMapping("/incident/{incidentId}")
    public Object byIncident(@PathVariable Long incidentId) { return service.byIncident(incidentId); }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','NOC_MANAGER','NETWORK_ENGINEER')")
    public Object create(@Valid @RequestBody RestorationCreate request,
                         org.springframework.security.core.Authentication authentication) {
        return service.create(request, authentication.getName());
    }

    @PatchMapping("/{id}/verify")
    @PreAuthorize("hasAnyRole('ADMIN','NOC_MANAGER')")
    public Object verify(@PathVariable Long id, @Valid @RequestBody RestorationVerification request,
                         org.springframework.security.core.Authentication authentication) {
        return service.verify(id, request.approved(), request.notes(), authentication.getName());
    }


}
