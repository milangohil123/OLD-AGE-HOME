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
import java.util.List;


@Repository
public interface DonorRepository extends JpaRepository<Donor, Long>, JpaSpecificationExecutor<Donor> {

       @Query("SELECT d FROM Donor d WHERE " +
                     "LOWER(d.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "LOWER(COALESCE(d.mobile, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "LOWER(COALESCE(d.email, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))")
       Page<Donor> searchDonors(@Param("keyword") String keyword, Pageable pageable);

       List<Donor> findByFullNameIgnoreCase(String fullName);

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

       long countByCreatedAtBefore(java.time.LocalDateTime date);

       List<Donor> findByCreatedAtAfter(java.time.LocalDateTime date);

       @Query("SELECT COUNT(d) FROM Donor d WHERE d.donationDate >= :startDate AND d.donationDate <= :endDate")
       long countByDonationDateBetween(@Param("startDate") java.time.LocalDate startDate, @Param("endDate") java.time.LocalDate endDate);

       @Query("SELECT COALESCE(SUM(d.donationAmount), 0) FROM Donor d WHERE d.donationDate < :date")
       BigDecimal sumTotalDonationAmountBefore(@Param("date") java.time.LocalDate date);

       // --- Trend Sparkline queries ---
       @Query("SELECT CAST(d.createdAt AS date) as date, COUNT(d) as count FROM Donor d WHERE d.createdAt >= :startDate GROUP BY CAST(d.createdAt AS date) ORDER BY CAST(d.createdAt AS date) ASC")
       List<Object[]> countDonorsGroupedByDate(@Param("startDate") java.time.LocalDateTime startDate);

       @Query("SELECT d.donationDate as date, COUNT(d) as count FROM Donor d WHERE d.donationDate >= :startDate GROUP BY d.donationDate ORDER BY d.donationDate ASC")
       List<Object[]> countDonationsGroupedByDate(@Param("startDate") java.time.LocalDate startDate);

       @Query("SELECT d.donationDate as date, SUM(d.donationAmount) as total FROM Donor d WHERE d.donationDate >= :startDate GROUP BY d.donationDate ORDER BY d.donationDate ASC")
       List<Object[]> sumDonationsGroupedByDate(@Param("startDate") java.time.LocalDate startDate);
}
