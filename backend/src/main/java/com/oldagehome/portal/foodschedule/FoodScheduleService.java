package com.oldagehome.portal.foodschedule;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface for Food Schedule business logic.
 */
public interface FoodScheduleService {

    // ── CRUD ──────────────────────────────────────────────────────────────────

    /** Save a new food schedule record. */
    FoodSchedule save(FoodScheduleDTO dto, String username);

    /** Update an existing food schedule record. */
    FoodSchedule update(Long id, FoodScheduleDTO dto, String username);

    /** Delete a food schedule record by ID. */
    void delete(Long id, String username);

    /** Get a single record by ID. */
    Optional<FoodSchedule> findById(Long id);

    // ── Schedule Queries ──────────────────────────────────────────────────────

    /** Get all records for today ordered by serving time. */
    List<FoodSchedule> findTodaysSchedule();

    /**
     * Get records for the last 7 days (excluding today),
     * grouped by date descending.
     */
    Map<LocalDate, List<FoodSchedule>> findLastSevenDays();

    // ── Search (existing — unchanged) ─────────────────────────────────────────

    /** Search schedule history with optional filters. Returns full list (no pagination). */
    List<FoodSchedule> searchSchedule(LocalDate fromDate,
                                       LocalDate toDate,
                                       MealType mealType,
                                       SponsorshipType sponsorshipType,
                                       String donorKeyword);

    // ── Search (new — backend-paginated, extended filters) ────────────────────

    /**
     * Paginated history search for the History Table.
     * Moves pagination, sorting, and extended filtering to the database layer.
     * Amount range filters (minAmount, maxAmount) are new; all other filters mirror
     * the existing searchSchedule parameters.
     */
    Page<FoodSchedule> searchSchedulePaged(LocalDate fromDate,
                                            LocalDate toDate,
                                            MealType mealType,
                                            SponsorshipType sponsorshipType,
                                            String donorKeyword,
                                            BigDecimal minAmount,
                                            BigDecimal maxAmount,
                                            Pageable pageable);

    // ── Stats ─────────────────────────────────────────────────────────────────

    /** Get basic dashboard statistics for today. (Preserved — used by existing callers.) */
    FoodScheduleStatsDTO getTodayStats();

    /**
     * Get enriched statistics including:
     *  — All existing today stats
     *  — Next scheduled meal (for KPI card)
     *  — Today's menu item count (for KPI card)
     *  — Weekly / monthly analytics totals
     * Business logic lives entirely in the service layer — frontend only displays values.
     */
    FoodScheduleStatsDTO getEnrichedStats();

    // ── Rate Lookup ───────────────────────────────────────────────────────────

    /**
     * Fetch donation rate from the food_donation_rates table.
     * Returns empty Optional if no matching active rate exists.
     */
    Optional<BigDecimal> getRateAmount(String mealType, String sponsorshipType);
}
