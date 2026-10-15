package com.oldagehome.portal.inventory;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class InventoryItemDTO {
    private String itemName;
    private String category; // Food, Medicine
    private double totalQuantity;
    private String unit;
    private int donorsCount;
    private LocalDate lastContributionDate;
    
    @Builder.Default
    private List<InventoryContributorDTO> contributors = new ArrayList<>();

    public InventoryItemDTO() {
    }

    @Builder
    public InventoryItemDTO(String itemName, String category, double totalQuantity, String unit,
                            int donorsCount, LocalDate lastContributionDate, List<InventoryContributorDTO> contributors) {
        this.itemName = itemName;
        this.category = category;
        this.totalQuantity = totalQuantity;
        this.unit = unit;
        this.donorsCount = donorsCount;
        this.lastContributionDate = lastContributionDate;
        this.contributors = contributors != null ? contributors : new ArrayList<>();
    }
    
    public String getFormattedTotalQuantity() {
        if (totalQuantity == (long) totalQuantity) {
            return String.format("%d %s", (long) totalQuantity, unit).trim();
        } else {
            return String.format("%.2f %s", totalQuantity, unit).trim();
        }
    }
}
