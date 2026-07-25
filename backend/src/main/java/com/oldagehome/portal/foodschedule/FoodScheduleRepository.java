package com.oldagehome.portal.foodschedule;

import com.oldagehome.portal.foodschedule.dto.FoodSponsorDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FoodScheduleRepository extends JpaRepository<FoodSchedule, Long> {

    @Query(value = "SELECT fs FROM FoodSchedule fs " +
           "LEFT JOIN FETCH fs.donor d " +
           "WHERE (:fromDate IS NULL OR fs.mealDate >= :fromDate) " +
           "AND (:toDate IS NULL OR fs.mealDate <= :toDate) " +
           "AND (:mealType IS NULL OR fs.mealType = :mealType) " +
           "AND (:donorId IS NULL OR (d IS NOT NULL AND d.id = :donorId)) " +
           "AND (:status IS NULL OR fs.status = :status) " +
           "ORDER BY fs.mealDate DESC, fs.mealType ASC",
           countQuery = "SELECT COUNT(fs) FROM FoodSchedule fs " +
           "LEFT JOIN fs.donor d " +
           "WHERE (:fromDate IS NULL OR fs.mealDate >= :fromDate) " +
           "AND (:toDate IS NULL OR fs.mealDate <= :toDate) " +
           "AND (:mealType IS NULL OR fs.mealType = :mealType) " +
           "AND (:donorId IS NULL OR (d IS NOT NULL AND d.id = :donorId)) " +
           "AND (:status IS NULL OR fs.status = :status)")
    Page<FoodSchedule> searchSchedules(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("mealType") MealType mealType,
            @Param("donorId") Long donorId,
            @Param("status") String status,
            Pageable pageable);

    boolean existsByMealDateAndMealType(LocalDate mealDate, MealType mealType);

    boolean existsByMealDateAndMealTypeAndIdNot(LocalDate mealDate, MealType mealType, Long id);

    long countByMealDate(LocalDate mealDate);

    long countByMealDateAndMealType(LocalDate mealDate, MealType mealType);

    long countByMealDateGreaterThanEqual(LocalDate mealDate);

    @Query("SELECT COUNT(DISTINCT fs.donor.id) FROM FoodSchedule fs WHERE fs.status = 'ACTIVE' AND fs.donor IS NOT NULL")
    long countActiveFoodSponsors();

    // Fetch eligible food sponsors: donors who have a Cash Food Donation with amount > 0 and are ACTIVE
    @Query("SELECT new com.oldagehome.portal.foodschedule.dto.FoodSponsorDTO(d.id, d.fullName, don.donationAmount, don.donationDate) " +
           "FROM Donation don " +
           "JOIN don.donor d " +
           "WHERE d.donationCategory = 'Food Donation' " +
           "AND don.donationType = com.oldagehome.portal.donor.DonationType.CASH " +
           "AND don.donationAmount > 0 " +
           "AND d.status = com.oldagehome.portal.donor.DonorStatus.ACTIVE " +
           "ORDER BY don.donationDate DESC")
    List<FoodSponsorDTO> findEligibleFoodSponsors();
}
