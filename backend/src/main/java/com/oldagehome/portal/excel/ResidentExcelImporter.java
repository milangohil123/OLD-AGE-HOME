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

        // Check header row to determine column offset
        Row headerRow = sheet.getRow(0);
        boolean hasIdColumn = false;
        if (headerRow != null) {
            String firstColHeader = getCellValue(headerRow.getCell(0), dataFormatter).trim().toLowerCase();
            // Exact match or matches exactly 'id' or 'resident id'
            if (firstColHeader.equals("id") || firstColHeader.equals("resident id") || firstColHeader.equals("resident_id")) {
                hasIdColumn = true;
            }
        }
        
        int nameIdx = hasIdColumn ? 1 : 0;
        int genderIdx = hasIdColumn ? 2 : 1;
        int dobIdx = hasIdColumn ? 3 : 2;
        int mobileIdx = hasIdColumn ? 4 : 3;
        int guardianIdx = hasIdColumn ? 5 : 4;
        int guardianPhoneIdx = hasIdColumn ? 6 : 5;

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            ResidentImportDTO dto = new ResidentImportDTO();
            dto.setRowNum(i + 1);

            // Column: Full Name
            dto.setFullName(getCellValue(row.getCell(nameIdx), dataFormatter).trim());
            
            // Column: Gender
            String rawGender = getCellValue(row.getCell(genderIdx), dataFormatter).trim().toUpperCase();
            dto.setGender(rawGender);

            // Column: Date of Birth
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

            // Column: Mobile
            dto.setMobile(getCellValue(row.getCell(mobileIdx), dataFormatter).trim());
            // Column: Guardian Name
            dto.setGuardianName(getCellValue(row.getCell(guardianIdx), dataFormatter).trim());
            // Column: Guardian Phone
            dto.setGuardianPhone(getCellValue(row.getCell(guardianPhoneIdx), dataFormatter).trim());

            // Default defaults for other fields
            dto.setGuardianEmail("");
            dto.setGuardianAddress("");
            dto.setRoomNumber("");
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
            // Room Number is no longer mandatory for bulk import

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
