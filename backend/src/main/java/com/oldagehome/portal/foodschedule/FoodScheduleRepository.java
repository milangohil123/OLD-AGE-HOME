package com.oldagehome.portal.foodschedule;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FoodScheduleRepository extends JpaRepository<FoodSchedule, Long> {

    // ===============================
    // Today's Schedule
    // ===============================

    @EntityGraph(attributePaths = { "donor" })
    List<FoodSchedule> findByScheduleDateOrderByServingTimeAsc(LocalDate date);

    // ===============================
    // Date Range
    // ===============================

    @EntityGraph(attributePaths = { "donor" })
    List<FoodSchedule> findByScheduleDateBetweenOrderByScheduleDateDescServingTimeAsc(
            LocalDate from,
            LocalDate to);

    // ===============================
    // Dashboard Stats
    // ===============================

    long countByScheduleDate(LocalDate date);

    @Query("""
            SELECT COUNT(DISTINCT fs.donor.id)
            FROM FoodSchedule fs
            WHERE fs.scheduleDate = :date
              AND fs.donor IS NOT NULL
            """)
    long countDistinctDonorsByDate(@Param("date") LocalDate date);

    @Query("""
            SELECT COALESCE(SUM(fs.amount), 0)
            FROM FoodSchedule fs
            WHERE fs.scheduleDate = :date
            """)
    BigDecimal sumAmountByDate(@Param("date") LocalDate date);

    @Query("""
            SELECT COUNT(fs)
            FROM FoodSchedule fs
            WHERE fs.scheduleDate > :today
            """)
    long countUpcomingMeals(@Param("today") LocalDate today);

    @Query("""
            SELECT fs
            FROM FoodSchedule fs
            WHERE
                (fs.scheduleDate = :today AND fs.servingTime > :now)
                OR fs.scheduleDate > :today
            ORDER BY fs.scheduleDate ASC, fs.servingTime ASC
            """)
    List<FoodSchedule> findNextUpcomingMeals(
            @Param("today") LocalDate today,
            @Param("now") LocalTime now,
            Pageable pageable);

    @Query("""
            SELECT COUNT(fs)
            FROM FoodSchedule fs
            WHERE fs.scheduleDate BETWEEN :from AND :to
            """)
    long countByScheduleDateBetween(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            SELECT COUNT(DISTINCT fs.donor.id)
            FROM FoodSchedule fs
            WHERE fs.scheduleDate BETWEEN :from AND :to
              AND fs.donor IS NOT NULL
            """)
    long countDistinctDonorsBetween(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            SELECT COALESCE(SUM(fs.amount), 0)
            FROM FoodSchedule fs
            WHERE fs.scheduleDate BETWEEN :from AND :to
            """)
    BigDecimal sumAmountBetween(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    // ===============================
    // History Search
    // ===============================

    @EntityGraph(attributePaths = { "donor" })
    @Query("""
            SELECT fs
            FROM FoodSchedule fs
            LEFT JOIN fs.donor d
            WHERE
                (:fromDate IS NULL OR fs.scheduleDate >= :fromDate)
            AND (:toDate IS NULL OR fs.scheduleDate <= :toDate)
            AND (:mealType IS NULL OR fs.mealType = :mealType)
            AND (:sponsorshipType IS NULL OR fs.sponsorshipType = :sponsorshipType)
            AND (
                :donorKeyword IS NULL
                OR (
                    d IS NOT NULL
                    AND d.fullName IS NOT NULL
                    AND LOWER(d.fullName)
                        LIKE LOWER(CONCAT('%', :donorKeyword, '%'))
                )
                OR (
                    fs.manualDonorName IS NOT NULL
                    AND LOWER(fs.manualDonorName)
                        LIKE LOWER(CONCAT('%', :donorKeyword, '%'))
                )
            )
            ORDER BY fs.scheduleDate DESC,
                     fs.servingTime ASC
            """)
    List<FoodSchedule> searchSchedule(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("mealType") MealType mealType,
            @Param("sponsorshipType") SponsorshipType sponsorshipType,
            @Param("donorKeyword") String donorKeyword);

    // ===============================
    // Paginated History Search
    // ===============================

    @EntityGraph(attributePaths = { "donor" })
    @Query("""
            SELECT fs
            FROM FoodSchedule fs
            LEFT JOIN fs.donor d
            WHERE
                (:fromDate IS NULL OR fs.scheduleDate >= :fromDate)
            AND (:toDate IS NULL OR fs.scheduleDate <= :toDate)
            AND (:mealType IS NULL OR fs.mealType = :mealType)
            AND (:sponsorshipType IS NULL OR fs.sponsorshipType = :sponsorshipType)
            AND (
                :donorKeyword IS NULL
                OR (
                    d IS NOT NULL
                    AND LOWER(d.fullName)
                        LIKE LOWER(CONCAT('%', :donorKeyword, '%'))
                )
                OR (
                    LOWER(fs.manualDonorName)
                        LIKE LOWER(CONCAT('%', :donorKeyword, '%'))
                )
            )
            AND (:minAmount IS NULL OR fs.amount >= :minAmount)
            AND (:maxAmount IS NULL OR fs.amount <= :maxAmount)
            ORDER BY fs.scheduleDate DESC,
                     fs.servingTime ASC
            """)
    Page<FoodSchedule> searchSchedulePaged(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("mealType") MealType mealType,
            @Param("sponsorshipType") SponsorshipType sponsorshipType,
            @Param("donorKeyword") String donorKeyword,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            Pageable pageable);

    // ===============================
    // Find By ID
    // ===============================

    @Override
    @EntityGraph(attributePaths = { "donor" })
    Optional<FoodSchedule> findById(Long id);
}