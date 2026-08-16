package com.oldagehome.portal.donor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DonationRepository extends JpaRepository<Donation, Long> {
    List<Donation> findByDonorIdOrderByDonationDateDesc(Long donorId);

    @org.springframework.data.jpa.repository.Query("SELECT new com.oldagehome.portal.dto.DonationTrendDTO(d.donationDate, SUM(d.donationAmount)) FROM Donation d WHERE d.donationDate >= :startDate GROUP BY d.donationDate ORDER BY d.donationDate")
    List<com.oldagehome.portal.dto.DonationTrendDTO> getDonationTrend(@org.springframework.data.repository.query.Param("startDate") java.time.LocalDate startDate);
}
