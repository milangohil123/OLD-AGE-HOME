package com.oldagehome.portal.foodschedule.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FoodSponsorDTO {
    private Long donorId;
    private String donorName;
    private BigDecimal donationAmount;
    private String paymentMethod;
    private LocalDate donationDate;

    public FoodSponsorDTO() {
    }

    @Builder
    public FoodSponsorDTO(Long donorId, String donorName, BigDecimal donationAmount, String paymentMethod, LocalDate donationDate) {
        this.donorId = donorId;
        this.donorName = donorName;
        this.donationAmount = donationAmount;
        this.paymentMethod = paymentMethod;
        this.donationDate = donationDate;
    }
}
