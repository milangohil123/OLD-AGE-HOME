package com.oldagehome.portal.foodschedule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Data Transfer Object for Food Schedule form binding and data transfer.
 * Decouples the form from the entity — uses donorId (Long) instead of a Donor object.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodScheduleDTO {

    private Long id;

    @NotNull(message = "Schedule date is required")
    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private LocalDate scheduleDate;

    @NotNull(message = "Meal type is required")
    private MealType mealType;

    @NotNull(message = "Serving time is required")
    @org.springframework.format.annotation.DateTimeFormat(pattern = "HH:mm")
    private LocalTime servingTime;

    @NotBlank(message = "Menu items are required")
    private String menuItems;

    /** Nullable — populated when donor exists in system */
    private Long donorId;

    /** Checkbox flag from UI: true when admin says donor is not in system */
    private boolean donorNotFound;

    /** Populated when donorNotFound = true */
    private String manualDonorName;

    @NotNull(message = "Sponsorship type is required")
    private SponsorshipType sponsorshipType;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    private String notes;
}
