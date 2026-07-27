package com.oldagehome.portal.foodschedule.dto;

import com.oldagehome.portal.foodschedule.MealType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodScheduleDTO {
    
    private Long id;

    @NotNull(message = "Meal Date is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate mealDate;

    @NotNull(message = "Meal Type is required")
    private MealType mealType;

    @NotBlank(message = "Menu Items are required")
    private String menuItems;

    private Long donorId;
    
    // For displaying donor name in list/view
    private String donorName;
    private String donorMobile;
    
    // Additional fields for display/read-only logic
    private BigDecimal donationAmount;
    private String paymentMethod;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate donationDate;

    private String remarks;
    
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
