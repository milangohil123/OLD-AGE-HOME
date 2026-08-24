package com.oldagehome.portal.donor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodDonationItemRepository extends JpaRepository<FoodDonationItem, Long> {

    List<FoodDonationItem> findByDonorIdOrderByDisplayOrderAsc(Long donorId);

    @Query("SELECT f.foodName, f.quantity, d.fullName, d.donationDate, d.donationType FROM FoodDonationItem f JOIN f.donor d")
    List<Object[]> findAllProjected();

    @Modifying
    @Query("DELETE FROM FoodDonationItem f WHERE f.donor.id = :donorId")
    void deleteByDonorId(@Param("donorId") Long donorId);
}
