package com.telecom.outage.controller;
import com.telecom.outage.dto.Requests.Login;import com.telecom.outage.service.AuthService;import jakarta.validation.Valid;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auth")
public class AuthController {private final AuthService service;public AuthController(AuthService s){service=s;}@PostMapping("/login") public Object login(@Valid @RequestBody Login r){return service.login(r);}}
