package com.oldagehome.portal.excel;

import com.oldagehome.portal.dto.ResidentImportDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ResidentExcelImporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    /**
     * Parses an input stream of an Excel file (.xlsx) and extracts a list of DTOs representing the rows.
     * Column order:
     *   0: Resident ID        (ignored on insert)
     *   1: Full Name          (required)
     *   2: Gender             (required: MALE/FEMALE/OTHER)
     *   3: Date of Birth      (required: dd-MM-yyyy)
     *   4: Mobile             (optional)
     *   5: Guardian Name      (required)
     *   6: Guardian Phone     (optional)
     */
    public static List<ResidentImportDTO> importResidents(InputStream is) throws Exception {
        List<ResidentImportDTO> dtos = new ArrayList<>();
        Workbook workbook = new XSSFWorkbook(is);
        Sheet sheet = workbook.getSheetAt(0);
        DataFormatter dataFormatter = new DataFormatter();

        int nameIdx = -1, genderIdx = -1, dobIdx = -1, mobileIdx = -1, guardianIdx = -1, guardianPhoneIdx = -1, roomIdx = -1;
        Row headerRow = sheet.getRow(0);
        if (headerRow != null) {
            for (int j = 0; j < headerRow.getLastCellNum(); j++) {
                Cell cell = headerRow.getCell(j);
                if (cell == null) continue;
                String header = getCellValue(cell, dataFormatter).trim().toLowerCase();
                if (header.isEmpty()) continue;
                
                if ((header.contains("name") || header.equals("full name")) && !header.contains("guardian")) nameIdx = j;
                else if (header.contains("gender")) genderIdx = j;
                else if (header.contains("dob") || header.contains("date of birth") || header.contains("birth")) dobIdx = j;
                else if (header.contains("mobile") || (header.contains("phone") && !header.contains("guardian"))) mobileIdx = j;
                else if (header.contains("guardian") && (header.contains("name") || header.equals("guardian"))) guardianIdx = j;
                else if (header.contains("guardian phone") || (header.contains("guardian") && header.contains("phone"))) guardianPhoneIdx = j;
                else if (header.contains("room")) roomIdx = j;
            }
        }
        
        // Fallback for missing headers (if indices are still -1, use original logic)
        if (nameIdx == -1) {
            boolean hasIdColumn = false;
            if (headerRow != null) {
                String firstColHeader = getCellValue(headerRow.getCell(0), dataFormatter).trim().toLowerCase();
                if (firstColHeader.equals("id") || firstColHeader.equals("resident id") || firstColHeader.equals("resident_id")) {
                    hasIdColumn = true;
                }
            }
            nameIdx = hasIdColumn ? 1 : 0;
            genderIdx = hasIdColumn ? 2 : 1;
            dobIdx = hasIdColumn ? 3 : 2;
            mobileIdx = hasIdColumn ? 4 : 3;
            guardianIdx = hasIdColumn ? 5 : 4;
            guardianPhoneIdx = hasIdColumn ? 6 : 5;
        }

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            ResidentImportDTO dto = new ResidentImportDTO();
            dto.setRowNum(i + 1);

            // Column: Full Name
            dto.setFullName(nameIdx >= 0 ? getCellValue(row.getCell(nameIdx), dataFormatter).trim() : "");
            
            // Column: Gender
            String rawGender = genderIdx >= 0 ? getCellValue(row.getCell(genderIdx), dataFormatter).trim().toUpperCase() : "";
            dto.setGender(rawGender);

            // Column: Date of Birth
            if (dobIdx >= 0) {
                Cell dobCell = row.getCell(dobIdx);
                if (dobCell != null) {
                    if (dobCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(dobCell)) {
                        Date date = dobCell.getDateCellValue();
                        dto.setDateOfBirth(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                    } else {
                        String dobStr = getCellValue(dobCell, dataFormatter).trim();
                        dto.setDateOfBirthString(dobStr);
                        try {
                            if (!dobStr.isEmpty()) {
                                dto.setDateOfBirth(LocalDate.parse(dobStr, DATE_FORMATTER));
                            }
                        } catch (DateTimeParseException e) {
                            dto.setValid(false);
                            dto.setErrorMessage("Invalid Date format. Use dd-MM-yyyy. ");
                        }
                    }
                }
            }

            // Column: Mobile
            String rawMobile = mobileIdx >= 0 ? getCellValue(row.getCell(mobileIdx), dataFormatter).trim() : "";
            dto.setMobile(rawMobile.replaceAll("[^0-9]", ""));
            // Column: Guardian Name
            dto.setGuardianName(guardianIdx >= 0 ? getCellValue(row.getCell(guardianIdx), dataFormatter).trim() : "");
            // Column: Guardian Phone
            String rawGPhone = guardianPhoneIdx >= 0 ? getCellValue(row.getCell(guardianPhoneIdx), dataFormatter).trim() : "";
            dto.setGuardianPhone(rawGPhone.replaceAll("[^0-9]", ""));

            // Default defaults for other fields
            dto.setGuardianEmail("");
            dto.setGuardianAddress("");
            
            String parsedRoom = roomIdx >= 0 ? getCellValue(row.getCell(roomIdx), dataFormatter).trim() : "";
            dto.setRoomNumber(parsedRoom.isEmpty() ? "TBD" : parsedRoom);
            
            dto.setMedicalPrescription("");
            dto.setOccupation("");
            dto.setDisability("");
            dto.setAadhaarNumber("");

            // ─── Validation ───────────────────────────────────────────────────────────
            StringBuilder errorBuilder = new StringBuilder(dto.getErrorMessage() == null ? "" : dto.getErrorMessage());

            if (dto.getFullName().isEmpty()) {
                dto.setValid(false);
                errorBuilder.append("Full name is required. ");
            }
            if (dto.getGender().isEmpty()) {
                dto.setValid(false);
                errorBuilder.append("Gender is required. ");
            } else if (!dto.getGender().equals("MALE") && !dto.getGender().equals("FEMALE") && !dto.getGender().equals("OTHER")) {
                dto.setValid(false);
                errorBuilder.append("Gender must be MALE, FEMALE, or OTHER. ");
            }
            if (dto.getDateOfBirth() == null && dto.isValid()) {
                dto.setValid(false);
                errorBuilder.append("Date of Birth is required. ");
            } else if (dto.getDateOfBirth() != null && dto.getDateOfBirth().isAfter(LocalDate.now())) {
                dto.setValid(false);
                errorBuilder.append("Date of Birth cannot be in the future. ");
            }
            if (dto.getGuardianName() == null || dto.getGuardianName().isEmpty()) {
                dto.setValid(false);
                errorBuilder.append("Guardian Name is required. ");
            }
            
            if (dto.getMobile() != null && !dto.getMobile().isEmpty()) {
                if (dto.getMobile().length() < 10 || dto.getMobile().length() > 15) {
                    dto.setValid(false);
                    errorBuilder.append("Mobile number must be between 10 and 15 digits. ");
                }
            }
            
            if (dto.getGuardianPhone() != null && !dto.getGuardianPhone().isEmpty()) {
                if (dto.getGuardianPhone().length() < 10 || dto.getGuardianPhone().length() > 15) {
                    dto.setValid(false);
                    errorBuilder.append("Guardian phone must be between 10 and 15 digits. ");
                }
            }
            
            // Room Number defaults to TBD if empty, so it's handled

            dto.setErrorMessage(errorBuilder.toString().trim());
            dtos.add(dto);
        }
        workbook.close();
        return dtos;
    }

    private static String getCellValue(Cell cell, DataFormatter formatter) {
        if (cell == null) return "";
        return formatter.formatCellValue(cell);
    }
}
