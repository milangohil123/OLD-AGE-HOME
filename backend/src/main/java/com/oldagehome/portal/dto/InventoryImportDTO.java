package com.oldagehome.portal.dto;

import com.oldagehome.portal.inventory.InventoryStatus;
import com.oldagehome.portal.inventory.MedicineCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object used when importing inventory records from Excel.
 * Each instance represents one row in the uploaded spreadsheet.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryImportDTO {

    private int rowNum;

    // Excel columns
    private String medicineCode;
    private String medicineName;
    private MedicineCategory category;
    private String manufacturer;
    private String supplier;
    private String batchNumber;
    private String purchaseDateString;
    private LocalDate purchaseDate;
    private String expiryDateString;
    private LocalDate expiryDate;
    private Integer quantity;
    private Integer minimumStock;
    private BigDecimal unitPrice;
    private String rackLocation;
    private String notes;

    // Row-level import status tracking
    private boolean valid = true;
    private String errorMessage = "";
}
