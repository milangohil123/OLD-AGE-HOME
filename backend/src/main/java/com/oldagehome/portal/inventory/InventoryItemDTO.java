package com.oldagehome.portal.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItemDTO {
    private String itemName;
    private String category; // Food, Medicine
    private double totalQuantity;
    private String unit;
    private int donorsCount;
    private LocalDate lastContributionDate;
    
    @Builder.Default
    private List<InventoryContributorDTO> contributors = new ArrayList<>();
    
    public String getFormattedTotalQuantity() {
        if (totalQuantity == (long) totalQuantity) {
            return String.format("%d %s", (long) totalQuantity, unit).trim();
        } else {
            return String.format("%.2f %s", totalQuantity, unit).trim();
        }
    }
}
