package com.errortracking.ingestionservice.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    private UUID id;

    private String name;

    @Column(name = "api_key")
    private String apiKey;

    private Boolean isActive;

    // Getters and Setters — IntelliJ me class ke andar right-click → Generate → Getter and Setter → sab select karke bana le
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}