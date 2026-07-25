package com.oldagehome.portal.foodschedule.dto;

import com.oldagehome.portal.foodschedule.MealType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchDTO {
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fromDate;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate toDate;
    
    private MealType mealType;
    private Long donorId;
    private String status;
}
