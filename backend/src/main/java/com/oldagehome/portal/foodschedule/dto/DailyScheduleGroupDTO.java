package com.oldagehome.portal.foodschedule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Groups all FoodSchedule records for a single date together
 * with aggregate totals, used for the "Past 7 Days" accordion section.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyScheduleGroupDTO {

    private LocalDate date;

    /** Human-readable label, e.g. "Yesterday - 27 Jul 2026" or "26 Jul 2026" */
    private String label;

    private long totalMeals;

    /** Sum of donationAmount for all schedules on this date */
    private BigDecimal totalAmount;

    private List<FoodScheduleDTO> schedules;
}
