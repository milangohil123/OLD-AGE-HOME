package com.oldagehome.portal.foodschedule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodDashboardDTO {
    private long todaysMealsCount;
    private long todayBreakfastCount;
    private long todayLunchCount;
    private long todayDinnerCount;
    private long activeFoodSponsorsCount;
    private long upcomingSchedulesCount;
}
