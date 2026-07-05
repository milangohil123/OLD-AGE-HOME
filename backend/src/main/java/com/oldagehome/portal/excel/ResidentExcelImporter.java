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
     *   0: Resident ID        (required)
     *   1: Full Name          (required)
     *   2: Gender             (required: MALE/FEMALE/OTHER)
     *   3: Date of Birth      (required: dd-MM-yyyy)
     *   4: Mobile             (optional)
     *   5: Guardian Name      (required)
     *   6: Guardian Phone     (optional)
     *   7: Guardian Email     (optional)
     *   8: Guardian Address   (optional)
     *   9: Room Number        (required)
     *  10: Medical Prescription (optional)
     *  11: Occupation         (optional)
     *  12: Disability         (optional)
     *  13: Aadhaar Number     (optional, 12 digits)
     */
    public static List<ResidentImportDTO> importResidents(InputStream is) throws Exception {
        List<ResidentImportDTO> dtos = new ArrayList<>();
        Workbook workbook = new XSSFWorkbook(is);
        Sheet sheet = workbook.getSheetAt(0);
        DataFormatter dataFormatter = new DataFormatter();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            ResidentImportDTO dto = new ResidentImportDTO();
            dto.setRowNum(i + 1);

            // Column 0: Resident ID
            dto.setResidentId(getCellValue(row.getCell(0), dataFormatter).trim());
            // Column 1: Full Name
            dto.setFullName(getCellValue(row.getCell(1), dataFormatter).trim());
            // Column 2: Gender
            dto.setGender(getCellValue(row.getCell(2), dataFormatter).trim().toUpperCase());

            // Column 3: Date of Birth
            Cell dobCell = row.getCell(3);
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
                        dto.setErrorMessage("Invalid Date format. Use dd-MM-yyyy.");
                    }
                }
            }

            // Column 4: Mobile
            dto.setMobile(getCellValue(row.getCell(4), dataFormatter).trim());
            // Column 5: Guardian Name
            dto.setGuardianName(getCellValue(row.getCell(5), dataFormatter).trim());
            // Column 6: Guardian Phone
            dto.setGuardianPhone(getCellValue(row.getCell(6), dataFormatter).trim());
            // Column 7: Guardian Email
            dto.setGuardianEmail(getCellValue(row.getCell(7), dataFormatter).trim());
            // Column 8: Guardian Address
            dto.setGuardianAddress(getCellValue(row.getCell(8), dataFormatter).trim());
            // Column 9: Room Number
            dto.setRoomNumber(getCellValue(row.getCell(9), dataFormatter).trim());
            // Column 10: Medical Prescription
            dto.setMedicalPrescription(getCellValue(row.getCell(10), dataFormatter).trim());
            // Column 11: Occupation
            dto.setOccupation(getCellValue(row.getCell(11), dataFormatter).trim());
            // Column 12: Disability
            dto.setDisability(getCellValue(row.getCell(12), dataFormatter).trim());
            // Column 13: Aadhaar Number
            dto.setAadhaarNumber(getCellValue(row.getCell(13), dataFormatter).trim());

            // ─── Validation ───────────────────────────────────────────────────────────
            StringBuilder errorBuilder = new StringBuilder(dto.getErrorMessage());

            if (dto.getResidentId().isEmpty()) {
                dto.setValid(false);
                errorBuilder.append("Resident ID is required. ");
            }
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
            if (dto.getRoomNumber() == null || dto.getRoomNumber().isEmpty()) {
                dto.setValid(false);
                errorBuilder.append("Room Number is required. ");
            }
            if (!dto.getAadhaarNumber().isEmpty() && !dto.getAadhaarNumber().matches("[0-9]{12}")) {
                dto.setValid(false);
                errorBuilder.append("Aadhaar Number must be exactly 12 digits. ");
            }

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
