package com.oldagehome.portal.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:username IS NULL OR LOWER(a.username) LIKE LOWER(CONCAT('%', :username, '%'))) AND " +
           "(:module IS NULL OR a.module = :module) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:success IS NULL OR a.success = :success) AND " +
           "(:startDate IS NULL OR a.timestamp >= :startDate) AND " +
           "(:endDate IS NULL OR a.timestamp <= :endDate)")
    Page<AuditLog> filterLogs(
            @Param("username") String username,
            @Param("module") AuditModule module,
            @Param("action") AuditAction action,
            @Param("success") Boolean success,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE " +
           "LOWER(a.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.entityName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.ipAddress) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<AuditLog> searchLogs(@Param("keyword") String keyword, Pageable pageable);

    long countByTimestampAfter(LocalDateTime startOfDay);

    long countByTimestampAfterAndSuccess(LocalDateTime startOfDay, boolean success);

    Optional<AuditLog> findFirstByActionOrderByTimestampDesc(AuditAction action);
}
