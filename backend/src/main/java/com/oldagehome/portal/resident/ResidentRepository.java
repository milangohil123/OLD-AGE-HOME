package com.oldagehome.portal.resident;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResidentRepository extends JpaRepository<Resident, Long>, JpaSpecificationExecutor<Resident> {

    Optional<Resident> findByResidentId(String residentId);

    boolean existsByResidentId(String residentId);

    /**
     * Search residents by full name, resident ID, guardian name, mobile, guardian phone, or room number.
     * Supports case-insensitive searching and matches substrings.
     */
    @Query("SELECT r FROM Resident r WHERE " +
           "LOWER(r.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(r.residentId) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(r.guardianName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(r.mobile) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(r.guardianPhone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(COALESCE(r.roomNumber, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Resident> searchResidents(@Param("keyword") String keyword, Pageable pageable);
}
