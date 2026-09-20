package com.errortracking.ingestionservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "errors") // SQL table ka naam
public class ErrorEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // BIGSERIAL ke liye
    private Long id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "fingerprint", length = 64, nullable = false)
    private String fingerprint;

    @Column(name = "error_type", nullable = false)
    private String errorType;

    @Column(name = "error_message", columnDefinition = "TEXT", nullable = false)
    private String errorMessage;

    @Column(name = "stack_trace", columnDefinition = "TEXT", nullable = false)
    private String stackTrace;

    @Column(name = "environment", length = 50)
    private String environment;

    @Column(name = "service_name")
    private String serviceName;

    @Column(name = "host_name")
    private String hostName;

    @Column(name = "app_version", length = 50)
    private String appVersion;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "occurred_at", nullable = false)
    private ZonedDateTime occurredAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;
}