package com.oldagehome.portal.foodschedule;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Lightweight stats DTO for the Food Schedule dashboard cards.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodScheduleStatsDTO {
    private long todayMealsCount;
    private long todayFoodDonorsCount;
    private BigDecimal todayDonationAmount;
    private long upcomingMealsCount;
}
