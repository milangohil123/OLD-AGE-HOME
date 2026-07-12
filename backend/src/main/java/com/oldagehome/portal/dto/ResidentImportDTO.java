package com.oldagehome.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResidentImportDTO {
    private int rowNum;
    private String fullName;
    private String gender;
    private String dateOfBirthString;
    private LocalDate dateOfBirth;
    private String mobile;
    private String guardianName;
    private String guardianPhone;
    private String guardianEmail;
    private String guardianAddress;
    private String medicalPrescription;
    private String roomNumber;
    private String occupation;
    private String disability;
    private String aadhaarNumber;

    // Status tracking for row-level warnings during Excel import
    private boolean valid = true;
    private String errorMessage = "";
}
