package com.oldagehome.portal.foodschedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface FoodScheduleRepository extends JpaRepository<FoodSchedule, Long> {

       // ── Today's schedule ──────────────────────────────────────────────────────

       List<FoodSchedule> findByScheduleDateOrderByServingTimeAsc(LocalDate date);

       // ── Date range ────────────────────────────────────────────────────────────

       List<FoodSchedule> findByScheduleDateBetweenOrderByScheduleDateDescServingTimeAsc(
                     LocalDate from, LocalDate to);

       // ── Dashboard stats ───────────────────────────────────────────────────────

       long countByScheduleDate(LocalDate date);

       @Query("""
                     SELECT COUNT(DISTINCT fs.donor.id)
                     FROM FoodSchedule fs
                     WHERE fs.scheduleDate = :date
                       AND fs.donor IS NOT NULL
                     """)
       long countDistinctDonorsByDate(@Param("date") LocalDate date);

       @Query("SELECT COALESCE(SUM(fs.amount), 0) FROM FoodSchedule fs WHERE fs.scheduleDate = :date")
       BigDecimal sumAmountByDate(@Param("date") LocalDate date);

       @Query("SELECT COUNT(fs) FROM FoodSchedule fs WHERE fs.scheduleDate > :today")
       long countUpcomingMeals(@Param("today") LocalDate today);

       // ── History search ────────────────────────────────────────────────────────

       @Query("SELECT fs FROM FoodSchedule fs " +
                     "WHERE (:fromDate IS NULL OR fs.scheduleDate >= :fromDate) " +
                     "AND (:toDate IS NULL OR fs.scheduleDate <= :toDate) " +
                     "AND (:mealType IS NULL OR fs.mealType = :mealType) " +
                     "AND (:sponsorshipType IS NULL OR fs.sponsorshipType = :sponsorshipType) " +
                     "AND (:donorKeyword IS NULL OR LOWER(fs.donor.fullName) LIKE LOWER(CONCAT('%', :donorKeyword, '%')) "
                     +
                     "     OR LOWER(fs.manualDonorName) LIKE LOWER(CONCAT('%', :donorKeyword, '%'))) " +
                     "ORDER BY fs.scheduleDate DESC, fs.servingTime ASC")
       List<FoodSchedule> searchSchedule(
                     @Param("fromDate") LocalDate fromDate,
                     @Param("toDate") LocalDate toDate,
                     @Param("mealType") MealType mealType,
                     @Param("sponsorshipType") SponsorshipType sponsorshipType,
                     @Param("donorKeyword") String donorKeyword);
}
