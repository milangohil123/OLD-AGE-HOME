package com.oldagehome.portal.dto;

import com.oldagehome.portal.donor.DonorStatus;
import com.oldagehome.portal.donor.DonationType;
import com.oldagehome.portal.donor.DonationFrequency;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object used when importing donor records from Excel.
 * Each instance represents one row in the uploaded spreadsheet.
 */
@Data
public class DonorImportDTO {

    private int rowNum;

    // Excel columns
    private String fullName;
    private String gender; // Can be defaulted if not in sheet
    private String mobile;
    private String email;
    private String address;
    private DonationFrequency donationFrequency;
    private DonationType donationType;
    private String donationCategory;
    private String paymentMethod;
    private String transactionId;
    private String donationDateString;
    private LocalDate donationDate;
    private DonorStatus status;

    // Medicine columns
    private String medicineName;
    private BigDecimal medicinePrice;
    private LocalDate medicineExpiryDate;

    // Food columns
    private String foodName;
    private String foodQuantity;

    // Cash/UPI/Cheque column
    private BigDecimal donationAmount;

    // Row-level import status tracking
    @Builder.Default
    private boolean valid = true;
    @Builder.Default
    private String errorMessage = "";

    public DonorImportDTO() {
    }

    @Builder
    public DonorImportDTO(int rowNum, String fullName, String gender, String mobile, String email,
                          String address, DonationFrequency donationFrequency, DonationType donationType,
                          String donationCategory, String paymentMethod, String transactionId,
                          String donationDateString, LocalDate donationDate, DonorStatus status,
                          String medicineName, BigDecimal medicinePrice, LocalDate medicineExpiryDate,
                          String foodName, String foodQuantity, BigDecimal donationAmount,
                          boolean valid, String errorMessage) {
        this.rowNum = rowNum;
        this.fullName = fullName;
        this.gender = gender;
        this.mobile = mobile;
        this.email = email;
        this.address = address;
        this.donationFrequency = donationFrequency;
        this.donationType = donationType;
        this.donationCategory = donationCategory;
        this.paymentMethod = paymentMethod;
        this.transactionId = transactionId;
        this.donationDateString = donationDateString;
        this.donationDate = donationDate;
        this.status = status;
        this.medicineName = medicineName;
        this.medicinePrice = medicinePrice;
        this.medicineExpiryDate = medicineExpiryDate;
        this.foodName = foodName;
        this.foodQuantity = foodQuantity;
        this.donationAmount = donationAmount;
        this.valid = valid;
        this.errorMessage = errorMessage != null ? errorMessage : "";
    }
}
