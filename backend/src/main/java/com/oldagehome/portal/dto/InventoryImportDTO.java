package com.oldagehome.portal.dto;

import com.oldagehome.portal.inventory.InventoryStatus;
import com.oldagehome.portal.inventory.MedicineCategory;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object used when importing inventory records from Excel.
 * Each instance represents one row in the uploaded spreadsheet.
 */
@Data
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
    @Builder.Default
    private boolean valid = true;
    @Builder.Default
    private String errorMessage = "";

    public InventoryImportDTO() {
    }

    @Builder
    public InventoryImportDTO(int rowNum, String medicineCode, String medicineName, MedicineCategory category,
                              String manufacturer, String supplier, String batchNumber, String purchaseDateString,
                              LocalDate purchaseDate, String expiryDateString, LocalDate expiryDate,
                              Integer quantity, Integer minimumStock, BigDecimal unitPrice, String rackLocation,
                              String notes, boolean valid, String errorMessage) {
        this.rowNum = rowNum;
        this.medicineCode = medicineCode;
        this.medicineName = medicineName;
        this.category = category;
        this.manufacturer = manufacturer;
        this.supplier = supplier;
        this.batchNumber = batchNumber;
        this.purchaseDateString = purchaseDateString;
        this.purchaseDate = purchaseDate;
        this.expiryDateString = expiryDateString;
        this.expiryDate = expiryDate;
        this.quantity = quantity;
        this.minimumStock = minimumStock;
        this.unitPrice = unitPrice;
        this.rackLocation = rackLocation;
        this.notes = notes;
        this.valid = valid;
        this.errorMessage = errorMessage != null ? errorMessage : "";
    }
}
