package com.oldagehome.portal.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AuditService {

    AuditLog saveAuditLog(AuditLog auditLog);

        AuditLog getAuditLogById(Long id);

    Page<AuditLog> getAuditLogs(String keyword, Pageable pageable);

    Page<AuditLog> filterLogs(
            String username,
            String module,
            String action,
            Boolean success,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable);

    void deleteOldLogs(LocalDateTime beforeDate);

    byte[] exportLogs(List<AuditLog> logs, String format) throws IOException;

    long countTodayActivities();

    long countFailedActivities();

    long countSuccessfulActivities();

    long getTotalRecords();

    LocalDateTime getLatestLogin();

    LocalDateTime getLatestSystemUpdate();

    void logActivity(
            AuditModule module,
            AuditAction action,
            String description,
            String entityName,
            Long entityId,
            boolean success,
            String errorMessage);
}
