package com.oldagehome.portal.donor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicineDonationItemRepository extends JpaRepository<MedicineDonationItem, Long> {

    List<MedicineDonationItem> findByDonorIdOrderByDisplayOrderAsc(Long donorId);

    @Modifying
    @Query("DELETE FROM MedicineDonationItem m WHERE m.donor.id = :donorId")
    void deleteByDonorId(@Param("donorId") Long donorId);
}
