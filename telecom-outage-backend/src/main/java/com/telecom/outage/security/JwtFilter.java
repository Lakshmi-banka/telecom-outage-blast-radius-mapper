package com.telecom.outage.security;
import jakarta.servlet.*;import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
@Component
public class JwtFilter extends OncePerRequestFilter {
 private final JwtService jwt;private final CustomUserDetailsService users;
 public JwtFilter(JwtService j,CustomUserDetailsService u){jwt=j;users=u;}
 protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
  String h=req.getHeader("Authorization");
  if(h!=null&&h.startsWith("Bearer ")){String t=h.substring(7);if(jwt.valid(t)&&SecurityContextHolder.getContext().getAuthentication()==null){
   UserDetails d=users.loadUserByUsername(jwt.subject(t));var a=new UsernamePasswordAuthenticationToken(d,null,d.getAuthorities());a.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));SecurityContextHolder.getContext().setAuthentication(a);
  }}chain.doFilter(req,res);
 }
}
