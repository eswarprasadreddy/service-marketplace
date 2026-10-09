package com.servicehub.repository;

import com.servicehub.model.ServiceRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, String> {
    List<ServiceRequest> findByCustomerIdOrderByCreatedAtDesc(String customerId);
    List<ServiceRequest> findByStatusOrderByCreatedAtDesc(String status);
    List<ServiceRequest> findAllByOrderByCreatedAtDesc();
}
