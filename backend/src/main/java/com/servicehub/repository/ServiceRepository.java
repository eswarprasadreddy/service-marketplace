package com.servicehub.repository;

import com.servicehub.model.ServiceType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRepository extends JpaRepository<ServiceType, String> {
    List<ServiceType> findAllByOrderByNameAsc();
}
