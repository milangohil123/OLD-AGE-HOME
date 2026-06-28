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
     * Validates data values and marks row-level validation errors.
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

            // Validation checks
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
