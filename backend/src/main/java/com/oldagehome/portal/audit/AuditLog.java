package com.oldagehome.portal.audit;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String username;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Column(length = 50)
    private String role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuditModule module;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuditAction action;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "entity_name", length = 100)
    private String entityName;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(length = 150)
    private String browser;

    @Column(name = "operating_system", length = 150)
    private String operatingSystem;

    @Column(name = "request_url", length = 255)
    private String requestUrl;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private Boolean success;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public AuditLog() {
    }

    @Builder
    public AuditLog(Long id, String username, String fullName, String role, AuditModule module,
                    AuditAction action, String description, String entityName, Long entityId,
                    String ipAddress, String browser, String operatingSystem, String requestUrl,
                    String httpMethod, LocalDateTime timestamp, Boolean success, String errorMessage) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.module = module;
        this.action = action;
        this.description = description;
        this.entityName = entityName;
        this.entityId = entityId;
        this.ipAddress = ipAddress;
        this.browser = browser;
        this.operatingSystem = operatingSystem;
        this.requestUrl = requestUrl;
        this.httpMethod = httpMethod;
        this.timestamp = timestamp;
        this.success = success;
        this.errorMessage = errorMessage;
    }
}
