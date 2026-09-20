package com.errortracking.ingestionservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "error_groups")
public class ErrorGroup {

    @Id
    @Column(name = "fingerprint", length = 64)
    private String fingerprint;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "error_type", nullable = false)
    private String errorType;

    @Column(name = "error_message", columnDefinition = "TEXT", nullable = false)
    private String errorMessage;

    @Column(name = "first_seen", nullable = false)
    private ZonedDateTime firstSeen;

    @Column(name = "last_seen", nullable = false)
    private ZonedDateTime lastSeen;

    @Column(name = "occurrence_count")
    private Long occurrenceCount = 1L;

    @Column(name = "affected_users_count")
    private Integer affectedUsersCount = 0;

    @Column(name = "status", length = 20)
    private String status = "open";

    @Column(name = "ai_group_id")
    private UUID aiGroupId;
}