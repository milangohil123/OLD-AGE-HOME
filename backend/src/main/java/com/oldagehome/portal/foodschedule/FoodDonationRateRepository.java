package com.oldagehome.portal.foodschedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FoodDonationRateRepository extends JpaRepository<FoodDonationRate, Long> {

    /**
     * Finds the active donation rate for a given meal type and sponsorship type.
     * Returns empty Optional if no matching rate is configured.
     */
    Optional<FoodDonationRate> findByMealTypeAndSponsorshipTypeAndIsActiveTrue(
            String mealType, String sponsorshipType);
}
