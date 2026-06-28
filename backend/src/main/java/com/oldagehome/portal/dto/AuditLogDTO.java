package com.oldagehome.portal.dto;

import com.oldagehome.portal.audit.AuditAction;
import com.oldagehome.portal.audit.AuditModule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
}
