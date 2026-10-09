package com.servicehub.model;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(name = "reviews")
public class Review {
    @Id
    @Column(length = 36)
    private String id;
    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;
    @Column(name = "worker_id", nullable = false, length = 36)
    private String workerId;
    @Column(name = "request_id", nullable = false, unique = true, length = 36)
    private String requestId;
    @Column(nullable = false)
    private int rating;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String comment;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @PrePersist
    public void createDefaults() { if (id == null) id = UUID.randomUUID().toString(); if (createdAt == null) createdAt = LocalDateTime.now(); }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
