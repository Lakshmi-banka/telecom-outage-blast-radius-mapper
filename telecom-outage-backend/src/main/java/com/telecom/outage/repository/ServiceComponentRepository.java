package com.telecom.outage.repository;
import com.telecom.outage.entity.ServiceComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ServiceComponentRepository extends JpaRepository<ServiceComponent,Long> {
 List<ServiceComponent> findByService_Id(Long serviceId);
}
