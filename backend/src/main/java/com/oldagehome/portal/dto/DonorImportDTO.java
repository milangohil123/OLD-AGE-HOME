package com.oldagehome.portal.dto;

import com.oldagehome.portal.donor.DonorStatus;
import com.oldagehome.portal.donor.DonationType;
import com.oldagehome.portal.donor.DonationFrequency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object used when importing donor records from Excel.
 * Each instance represents one row in the uploaded spreadsheet.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    private boolean valid = true;
    private String errorMessage = "";
}
