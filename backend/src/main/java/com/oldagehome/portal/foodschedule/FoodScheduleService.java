package com.oldagehome.portal.foodschedule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface for Food Schedule business logic.
 */
public interface FoodScheduleService {

    /** Save a new food schedule record. */
    FoodSchedule save(FoodScheduleDTO dto, String username);

    /** Update an existing food schedule record. */
    FoodSchedule update(Long id, FoodScheduleDTO dto, String username);

    /** Delete a food schedule record by ID. */
    void delete(Long id, String username);

    /** Get a single record by ID. */
    Optional<FoodSchedule> findById(Long id);

    /** Get all records for today ordered by serving time. */
    List<FoodSchedule> findTodaysSchedule();

    /**
     * Get records for the last 7 days (excluding today),
     * grouped by date descending.
     */
    Map<LocalDate, List<FoodSchedule>> findLastSevenDays();

    /** Search schedule history with optional filters. */
    List<FoodSchedule> searchSchedule(LocalDate fromDate,
                                       LocalDate toDate,
                                       MealType mealType,
                                       SponsorshipType sponsorshipType,
                                       String donorKeyword);

    /** Get dashboard statistics for today. */
    FoodScheduleStatsDTO getTodayStats();

    /**
     * Fetch donation rate from the food_donation_rates table.
     * Returns empty Optional if no matching active rate exists.
     */
    Optional<BigDecimal> getRateAmount(String mealType, String sponsorshipType);
}
