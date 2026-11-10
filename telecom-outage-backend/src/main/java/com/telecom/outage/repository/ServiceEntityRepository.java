package com.telecom.outage.repository;
import com.telecom.outage.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ServiceEntityRepository extends JpaRepository<ServiceEntity,Long> {
 Optional<ServiceEntity> findByServiceCode(String serviceCode);
}
