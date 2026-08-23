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
           "WHERE (:#{#fromDate == null} = true OR fs.mealDate >= :fromDate) " +
           "AND (:#{#toDate == null} = true OR fs.mealDate <= :toDate) " +
           "AND (:#{#mealType == null} = true OR fs.mealType = :mealType) " +
           "AND (:#{#donorId == null} = true OR d.id = :donorId) " +
           "AND (:#{#status == null} = true OR fs.status = :status) " +
           "ORDER BY fs.mealDate DESC, fs.mealType ASC",
           countQuery = "SELECT COUNT(fs) FROM FoodSchedule fs " +
           "LEFT JOIN fs.donor d " +
           "WHERE (:#{#fromDate == null} = true OR fs.mealDate >= :fromDate) " +
           "AND (:#{#toDate == null} = true OR fs.mealDate <= :toDate) " +
           "AND (:#{#mealType == null} = true OR fs.mealType = :mealType) " +
           "AND (:#{#donorId == null} = true OR d.id = :donorId) " +
           "AND (:#{#status == null} = true OR fs.status = :status)")
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
    @Query("SELECT new com.oldagehome.portal.foodschedule.dto.FoodSponsorDTO(d.id, d.fullName, d.donationAmount, d.paymentMethod, d.donationDate) " +
           "FROM Donor d " +
           "WHERE LOWER(TRIM(d.donationCategory)) = 'food meal donation (scheme)' " +
           "AND LOWER(TRIM(CAST(d.donationType AS string))) IN ('cash', 'upi', 'cheque') " +
           "AND UPPER(TRIM(CAST(d.status AS string))) = 'ACTIVE' " +
           "AND d.donationAmount > 0 " +
           "ORDER BY d.fullName")
    List<FoodSponsorDTO> findEligibleFoodSponsors();

    // For Today's Food Schedule card
    @Query("SELECT fs FROM FoodSchedule fs LEFT JOIN FETCH fs.donor WHERE fs.mealDate = :date ORDER BY fs.mealType ASC")
    List<FoodSchedule> findByMealDateOrderByMealTypeAsc(@Param("date") LocalDate date);

    // For Past 7 Days accordion
    @Query("SELECT fs FROM FoodSchedule fs LEFT JOIN FETCH fs.donor " +
           "WHERE fs.mealDate >= :fromDate AND fs.mealDate <= :toDate " +
           "ORDER BY fs.mealDate DESC, fs.mealType ASC")
    List<FoodSchedule> findByDateRangeOrderByDateDescMealTypeAsc(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    // --- Trend Sparkline queries ---
    @Query("SELECT fs.mealDate as date, COUNT(fs) as count FROM FoodSchedule fs WHERE fs.mealDate >= :startDate GROUP BY fs.mealDate ORDER BY fs.mealDate ASC")
    List<Object[]> countMealsGroupedByDate(@Param("startDate") LocalDate startDate);

    @Query("SELECT fs.mealDate as date, COUNT(fs) as count FROM FoodSchedule fs WHERE fs.mealDate >= :startDate AND fs.mealType = :mealType GROUP BY fs.mealDate ORDER BY fs.mealDate ASC")
    List<Object[]> countMealsGroupedByDateAndType(@Param("startDate") LocalDate startDate, @Param("mealType") MealType mealType);
}
