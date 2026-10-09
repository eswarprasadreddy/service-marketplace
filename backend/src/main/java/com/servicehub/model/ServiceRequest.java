package com.servicehub.model;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(name = "service_requests")
public class ServiceRequest {
    @Id
    @Column(length = 36)
    private String id;
    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;
    @Column(name = "service_id", nullable = false, length = 36)
    private String serviceId;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    @Column(nullable = false, length = 255)
    private String location;
    @Column(nullable = false, length = 20)
    private String status = "OPEN";
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @PrePersist
    public void createDefaults() { if (id == null) id = UUID.randomUUID().toString(); if (createdAt == null) createdAt = LocalDateTime.now(); }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
