package com.telecom.outage.repository;
import com.telecom.outage.entity.ComponentDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ComponentDependencyRepository extends JpaRepository<ComponentDependency,Long> {
 List<ComponentDependency> findByActiveTrue();
}
