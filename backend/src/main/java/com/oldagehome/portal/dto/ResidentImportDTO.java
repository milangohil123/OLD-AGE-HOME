package com.oldagehome.portal.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
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
    @Builder.Default
    private boolean valid = true;
    @Builder.Default
    private String errorMessage = "";

    public ResidentImportDTO() {
    }

    @Builder
    public ResidentImportDTO(int rowNum, String fullName, String gender, String dateOfBirthString,
                             LocalDate dateOfBirth, String mobile, String guardianName, String guardianPhone,
                             String guardianEmail, String guardianAddress, String medicalPrescription,
                             String roomNumber, String occupation, String disability, String aadhaarNumber,
                             boolean valid, String errorMessage) {
        this.rowNum = rowNum;
        this.fullName = fullName;
        this.gender = gender;
        this.dateOfBirthString = dateOfBirthString;
        this.dateOfBirth = dateOfBirth;
        this.mobile = mobile;
        this.guardianName = guardianName;
        this.guardianPhone = guardianPhone;
        this.guardianEmail = guardianEmail;
        this.guardianAddress = guardianAddress;
        this.medicalPrescription = medicalPrescription;
        this.roomNumber = roomNumber;
        this.occupation = occupation;
        this.disability = disability;
        this.aadhaarNumber = aadhaarNumber;
        this.valid = valid;
        this.errorMessage = errorMessage != null ? errorMessage : "";
    }
}
