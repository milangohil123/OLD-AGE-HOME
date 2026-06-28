package com.oldagehome.portal.dto;

import com.oldagehome.portal.donor.DonorStatus;
import com.oldagehome.portal.donor.DonationType;
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
    private String donorId;
    private String fullName;
    private String gender;
    private String mobile;
    private String email;
    private DonationType donationType;
    private BigDecimal donationAmount;
    private String donationDateString;
    private LocalDate donationDate;
    private String paymentMethod;
    private DonorStatus status;

    // Row-level import status tracking
    private boolean valid = true;
    private String errorMessage = "";
}
