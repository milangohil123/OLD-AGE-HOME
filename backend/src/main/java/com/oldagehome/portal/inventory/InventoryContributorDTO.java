package com.oldagehome.portal.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryContributorDTO {
    private String donorName;
    private String quantity;
    private LocalDate donationDate;
    private String donationType;
}
