package com.oldagehome.portal.foodschedule.dto;

import com.oldagehome.portal.foodschedule.MealType;
import lombok.Builder;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class SearchDTO {
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fromDate;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate toDate;
    
    private MealType mealType;
    private Long donorId;
    private String status;

    public SearchDTO() {
    }

    @Builder
    public SearchDTO(LocalDate fromDate, LocalDate toDate, MealType mealType, Long donorId, String status) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.mealType = mealType;
        this.donorId = donorId;
        this.status = status;
    }
}
