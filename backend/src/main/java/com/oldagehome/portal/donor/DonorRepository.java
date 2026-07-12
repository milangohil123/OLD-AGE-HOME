package com.oldagehome.portal.donor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DonorRepository extends JpaRepository<Donor, Long>, JpaSpecificationExecutor<Donor> {

    @Query("SELECT d FROM Donor d WHERE " +
           "LOWER(d.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.mobile) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Donor> searchDonors(@Param("keyword") String keyword, Pageable pageable);

    // --- Dashboard statistics queries ---

    /** Count all donors. */
    long count();

    /** Count donations made today. */
    @Query("SELECT COUNT(d) FROM Donor d WHERE d.donationDate = :today")
    long countByDonationDate(@Param("today") LocalDate today);

    /** Count donations made in a given month/year. */
    @Query("SELECT COUNT(d) FROM Donor d WHERE YEAR(d.donationDate) = :year AND MONTH(d.donationDate) = :month")
    long countDonationsByMonth(@Param("year") int year, @Param("month") int month);

    /** Sum total donation amount across all donors. */
    @Query("SELECT COALESCE(SUM(d.donationAmount), 0) FROM Donor d")
    BigDecimal sumTotalDonationAmount();
}
