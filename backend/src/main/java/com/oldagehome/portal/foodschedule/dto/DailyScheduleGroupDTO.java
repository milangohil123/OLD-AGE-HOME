package com.oldagehome.portal.foodschedule.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Groups all FoodSchedule records for a single date together
 * with aggregate totals, used for the "Past 7 Days" accordion section.
 */
@Data
public class DailyScheduleGroupDTO {

    private LocalDate date;

    /** Human-readable label, e.g. "Yesterday - 27 Jul 2026" or "26 Jul 2026" */
    private String label;

    private long totalMeals;

    /** Sum of donationAmount for all schedules on this date */
    private BigDecimal totalAmount;

    private List<FoodScheduleDTO> schedules;

    public DailyScheduleGroupDTO() {
    }

    @Builder
    public DailyScheduleGroupDTO(LocalDate date, String label, long totalMeals, BigDecimal totalAmount, List<FoodScheduleDTO> schedules) {
        this.date = date;
        this.label = label;
        this.totalMeals = totalMeals;
        this.totalAmount = totalAmount;
        this.schedules = schedules;
    }
}
