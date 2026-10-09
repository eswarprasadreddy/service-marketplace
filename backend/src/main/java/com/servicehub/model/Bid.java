package com.servicehub.model;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(name = "bids")
public class Bid {
    @Id
    @Column(length = 36)
    private String id;
    @Column(name = "request_id", nullable = false, length = 36)
    private String requestId;
    @Column(name = "worker_id", nullable = false, length = 36)
    private String workerId;
    @Column(nullable = false)
    private double price;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    @Column(nullable = false, length = 20)
    private String status = "PENDING";
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @PrePersist
    public void createDefaults() { if (id == null) id = UUID.randomUUID().toString(); if (createdAt == null) createdAt = LocalDateTime.now(); }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
