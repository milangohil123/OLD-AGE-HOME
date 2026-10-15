package com.oldagehome.portal.dto;

import com.oldagehome.portal.audit.AuditAction;
import com.oldagehome.portal.audit.AuditModule;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLogDTO {
    private Long id;
    private String username;
    private String fullName;
    private String role;
    private AuditModule module;
    private AuditAction action;
    private String description;
    private String entityName;
    private Long entityId;
    private String ipAddress;
    private String browser;
    private String operatingSystem;
    private String requestUrl;
    private String httpMethod;
    private LocalDateTime timestamp;
    private Boolean success;
    private String errorMessage;

    public AuditLogDTO() {
    }

    @Builder
    public AuditLogDTO(Long id, String username, String fullName, String role, AuditModule module,
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
