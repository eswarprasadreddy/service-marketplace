package com.servicehub.model;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(name = "services")
public class ServiceType {
    @Id
    @Column(length = 36)
    private String id;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    @PrePersist
    public void createId() { if (id == null) id = UUID.randomUUID().toString(); }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
