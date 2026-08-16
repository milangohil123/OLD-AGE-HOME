package com.oldagehome.portal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DonationTrendDTO {
    
    private LocalDate date;
    private BigDecimal amount;

    public DonationTrendDTO() {
    }

    public DonationTrendDTO(LocalDate date, BigDecimal amount) {
        this.date = date;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount != null ? amount : BigDecimal.ZERO;
    }
}
