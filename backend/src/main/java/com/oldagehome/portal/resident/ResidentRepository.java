package com.oldagehome.portal.resident;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;



@Repository
public interface ResidentRepository extends JpaRepository<Resident, Long>, JpaSpecificationExecutor<Resident> {

    /**
     * Search residents by full name, resident ID, guardian name, mobile, guardian
     * phone, or room number.
     * Supports case-insensitive searching and matches substrings.
     */
    @Query("SELECT r FROM Resident r WHERE " +
            "LOWER(r.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(r.guardianName, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(r.mobile, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(r.guardianPhone, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(r.roomNumber, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Resident> searchResidents(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT r FROM Resident r WHERE r.fullName = :fullName AND r.dateOfBirth = :dateOfBirth AND r.mobile = :mobile AND r.bloodGroup = :bloodGroup")
    List<Resident> findPotentialDuplicates(@Param("fullName") String fullName, @Param("dateOfBirth") java.time.LocalDate dateOfBirth, @Param("mobile") String mobile, @Param("bloodGroup") String bloodGroup);

    long countByJoiningDateBefore(java.time.LocalDate date);
}
