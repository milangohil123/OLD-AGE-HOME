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
    private String residentId;
    private String fullName;
    private String gender;
    private String dateOfBirthString;
    private LocalDate dateOfBirth;
    private String mobile;
    private String guardianName;
    private String guardianPhone;
    
    // Status tracking for row-level warnings during Excel import
    private boolean valid = true;
    private String errorMessage = "";
}
