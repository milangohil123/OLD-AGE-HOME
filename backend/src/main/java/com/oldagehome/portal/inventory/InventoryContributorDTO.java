package com.oldagehome.portal.inventory;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
public class InventoryContributorDTO {
    private String donorName;
    private String quantity;
    private LocalDate donationDate;
    private String donationType;

    public InventoryContributorDTO() {
    }

    @Builder
    public InventoryContributorDTO(String donorName, String quantity, LocalDate donationDate, String donationType) {
        this.donorName = donorName;
        this.quantity = quantity;
        this.donationDate = donationDate;
        this.donationType = donationType;
    }
}
