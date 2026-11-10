package com.telecom.outage.repository;
import com.telecom.outage.entity.Component;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ComponentRepository extends JpaRepository<Component,Long> {
 Optional<Component> findByComponentCode(String componentCode);
}
