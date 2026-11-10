package com.telecom.outage.service;
import com.telecom.outage.dto.Requests.Login;
import com.telecom.outage.repository.UserRepository;
import com.telecom.outage.security.JwtService;
import org.springframework.security.authentication.*;
import org.springframework.stereotype.Service;
@Service
public class AuthService {
 private final AuthenticationManager manager;private final UserRepository users;private final JwtService jwt;
 public AuthService(AuthenticationManager m,UserRepository u,JwtService j){manager=m;users=u;jwt=j;}
 public Object login(Login r){manager.authenticate(new UsernamePasswordAuthenticationToken(r.email(),r.password()));var u=users.findByEmail(r.email()).orElseThrow();return java.util.Map.of("token",jwt.create(u.getEmail()),"tokenType","Bearer","email",u.getEmail(),"role",u.getRole().getRoleName());}
}
