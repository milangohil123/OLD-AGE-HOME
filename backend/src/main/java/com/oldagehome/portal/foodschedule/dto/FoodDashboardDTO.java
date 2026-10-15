package com.oldagehome.portal.foodschedule.dto;

import lombok.Builder;
import lombok.Data;

@Data
public class FoodDashboardDTO {
    private long todaysMealsCount;
    private long todayBreakfastCount;
    private long todayLunchCount;
    private long todayDinnerCount;
    private long activeFoodSponsorsCount;
    private long upcomingSchedulesCount;

    public FoodDashboardDTO() {
    }

    @Builder
    public FoodDashboardDTO(long todaysMealsCount, long todayBreakfastCount, long todayLunchCount,
                            long todayDinnerCount, long activeFoodSponsorsCount, long upcomingSchedulesCount) {
        this.todaysMealsCount = todaysMealsCount;
        this.todayBreakfastCount = todayBreakfastCount;
        this.todayLunchCount = todayLunchCount;
        this.todayDinnerCount = todayDinnerCount;
        this.activeFoodSponsorsCount = activeFoodSponsorsCount;
        this.upcomingSchedulesCount = upcomingSchedulesCount;
    }
}
