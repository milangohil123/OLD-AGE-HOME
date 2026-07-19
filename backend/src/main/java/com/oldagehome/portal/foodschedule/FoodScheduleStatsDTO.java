package com.oldagehome.portal.foodschedule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Lightweight stats DTO for the Food Schedule dashboard cards and analytics section.
 * The original four fields are preserved. Six new fields are added for:
 *  — Next Meal KPI (nextScheduledMeal, nextMealTime)
 *  — Today's Menu Items KPI (todayMenuItemCount)
 *  — Weekly / Monthly analytics (mealsThisWeek, sponsorsThisWeek, donationsThisMonth)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodScheduleStatsDTO {

    // ── Original fields (preserved, unchanged) ────────────────────────────────

    /** Number of meals scheduled for today. */
    private long todayMealsCount;

    /** Number of distinct food donors sponsoring today's meals. */
    private long todayFoodDonorsCount;

    /** Total donation amount received / pledged for today's meals. */
    private BigDecimal todayDonationAmount;

    /** Number of sponsored meals scheduled on future dates. */
    private long upcomingMealsCount;

    // ── New KPI fields ────────────────────────────────────────────────────────

    /** Display name of the next upcoming meal (e.g., "Dinner"). Empty string if none. */
    private String nextScheduledMeal;

    /** Formatted time of the next upcoming meal (e.g., "7:30 PM"). Empty string if none. */
    private String nextMealTime;

    /** Number of unique menu lines across all of today's meals. */
    private long todayMenuItemCount;

    // ── Analytics fields ──────────────────────────────────────────────────────

    /** Number of meals scheduled from Monday through Sunday of the current week. */
    private long mealsThisWeek;

    /** Number of distinct sponsors for the current week. */
    private long sponsorsThisWeek;

    /** Total donation amount for the current calendar month. */
    private BigDecimal donationsThisMonth;
}
