package com.telecom.outage.repository;
import com.telecom.outage.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface RoleRepository extends JpaRepository<Role,Long> {
 Optional<Role> findByRoleName(String roleName);
}
