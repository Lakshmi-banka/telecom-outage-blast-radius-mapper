package com.telecom.outage.security;
import com.telecom.outage.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
@Service
public class CustomUserDetailsService implements UserDetailsService {
 private final UserRepository users;
 public CustomUserDetailsService(UserRepository users){this.users=users;}
 public UserDetails loadUserByUsername(String email){
  var u=users.findByEmail(email).orElseThrow(()->new UsernameNotFoundException("User not found"));
  return User.withUsername(u.getEmail()).password(u.getPasswordHash()).authorities("ROLE_"+u.getRole().getRoleName()).disabled(!u.isActive()).build();
 }
}
