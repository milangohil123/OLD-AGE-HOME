package com.oldagehome.portal.excel;

import com.oldagehome.portal.donor.DonorStatus;
import com.oldagehome.portal.donor.DonationType;
import com.oldagehome.portal.dto.DonorImportDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Parses an uploaded Excel file (.xlsx) containing donor records.
 *
 * Expected column order:
 *  0 - Donor ID
 *  1 - Full Name
 *  2 - Gender         (MALE / FEMALE / OTHER)
 *  3 - Mobile
 *  4 - Email
 *  5 - Donation Type  (enum name)
 *  6 - Amount
 *  7 - Donation Date  (dd-MM-yyyy or numeric Excel date)
 *  8 - Payment Method
 *  9 - Status         (ACTIVE / INACTIVE / BLOCKED)
 */
public class DonorExcelImporter {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public static List<DonorImportDTO> importDonors(InputStream is) throws Exception {
        List<DonorImportDTO> dtos = new ArrayList<>();
        Workbook workbook = new XSSFWorkbook(is);
        Sheet sheet = workbook.getSheetAt(0);
        DataFormatter dataFormatter = new DataFormatter();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            DonorImportDTO dto = new DonorImportDTO();
            dto.setRowNum(i + 1);
            StringBuilder errors = new StringBuilder();

            // --- Column 0: Donor ID ---
            String donorId = getCellValue(row.getCell(0), dataFormatter).trim();
            dto.setDonorId(donorId);
            if (donorId.isEmpty()) {
                dto.setValid(false);
                errors.append("Donor ID is required. ");
            }

            // --- Column 1: Full Name ---
            String fullName = getCellValue(row.getCell(1), dataFormatter).trim();
            dto.setFullName(fullName);
            if (fullName.isEmpty()) {
                dto.setValid(false);
                errors.append("Full name is required. ");
            }

            // --- Column 2: Gender ---
            String gender = getCellValue(row.getCell(2), dataFormatter).trim().toUpperCase();
            dto.setGender(gender);
            if (gender.isEmpty()) {
                dto.setValid(false);
                errors.append("Gender is required. ");
            } else if (!gender.equals("MALE") && !gender.equals("FEMALE") && !gender.equals("OTHER")) {
                dto.setValid(false);
                errors.append("Gender must be MALE, FEMALE, or OTHER. ");
            }

            // --- Column 3: Mobile ---
            dto.setMobile(getCellValue(row.getCell(3), dataFormatter).trim());

            // --- Column 4: Email ---
            dto.setEmail(getCellValue(row.getCell(4), dataFormatter).trim());

            // --- Column 5: Donation Type ---
            String donationTypeStr = getCellValue(row.getCell(5), dataFormatter).trim();
            if (!donationTypeStr.isEmpty()) {
                DonationType parsedType = parseDonationType(donationTypeStr);
                if (parsedType == null) {
                    dto.setValid(false);
                    errors.append("Invalid Donation Type: '").append(donationTypeStr)
                          .append("'. Supported values: ONE_TIME, MONTHLY, FOOD, MEDICINE, CLOTHES, FESTIVAL, GENERAL, MEDICAL_EQUIPMENT, CASH, ONLINE, CHEQUE, UPI, BANK_TRANSFER, GOODS, OTHER. ");
                } else {
                    dto.setDonationType(parsedType);
                }
            } else {
                dto.setValid(false);
                errors.append("Donation Type is required. ");
            }

            // --- Column 6: Donation Amount ---
            Cell amountCell = row.getCell(6);
            if (amountCell != null && amountCell.getCellType() == CellType.NUMERIC) {
                double amountVal = amountCell.getNumericCellValue();
                if (amountVal < 0) {
                    dto.setValid(false);
                    errors.append("Donation amount cannot be negative. ");
                } else {
                    dto.setDonationAmount(BigDecimal.valueOf(amountVal));
                }
            } else {
                String amountStr = getCellValue(amountCell, dataFormatter).trim();
                if (!amountStr.isEmpty()) {
                    try {
                        BigDecimal amount = new BigDecimal(amountStr);
                        if (amount.compareTo(BigDecimal.ZERO) < 0) {
                            dto.setValid(false);
                            errors.append("Donation amount cannot be negative. ");
                        } else {
                            dto.setDonationAmount(amount);
                        }
                    } catch (NumberFormatException e) {
                        dto.setValid(false);
                        errors.append("Invalid donation amount format. ");
                    }
                } else {
                    dto.setValid(false);
                    errors.append("Donation amount is required. ");
                }
            }

            // --- Column 7: Donation Date ---
            Cell dateCell = row.getCell(7);
            if (dateCell != null) {
                if (dateCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(dateCell)) {
                    Date date = dateCell.getDateCellValue();
                    dto.setDonationDate(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                } else {
                    String dateStr = getCellValue(dateCell, dataFormatter).trim();
                    dto.setDonationDateString(dateStr);
                    if (!dateStr.isEmpty()) {
                        try {
                            dto.setDonationDate(LocalDate.parse(dateStr, DATE_FMT));
                        } catch (DateTimeParseException e) {
                            dto.setValid(false);
                            errors.append("Invalid date format. Use dd-MM-yyyy. ");
                        }
                    }
                }
            }
            // Default donation date to today if missing
            if (dto.getDonationDate() == null && dto.isValid()) {
                dto.setDonationDate(LocalDate.now());
            }

            // --- Column 8: Payment Method ---
            dto.setPaymentMethod(getCellValue(row.getCell(8), dataFormatter).trim());

            // --- Column 9: Status ---
            String statusStr = getCellValue(row.getCell(9), dataFormatter).trim().toUpperCase();
            if (!statusStr.isEmpty()) {
                try {
                    dto.setStatus(DonorStatus.valueOf(statusStr));
                } catch (IllegalArgumentException e) {
                    dto.setStatus(DonorStatus.ACTIVE); // default
                }
            } else {
                dto.setStatus(DonorStatus.ACTIVE);
            }

            dto.setErrorMessage(errors.toString().trim());
            dtos.add(dto);
        }

        workbook.close();
        return dtos;
    }

    private static String getCellValue(Cell cell, DataFormatter formatter) {
        if (cell == null) return "";
        return formatter.formatCellValue(cell);
    }

    /**
     * Converts an Excel cell string to a {@link DonationType} enum constant.
     *
     * Normalisation steps:
     * 1. Trim surrounding whitespace.
     * 2. Convert to UPPER_CASE.
     * 3. Replace one-or-more spaces with an underscore so that "One Time",
     *    "ONE TIME" and "ONE_TIME" all map to {@code ONE_TIME}.
     * 4. Try {@link DonationType#valueOf(String)} with the normalised string.
     *
     * Returns {@code null} if no match is found.
     */
    private static DonationType parseDonationType(String raw) {
        if (raw == null || raw.isBlank()) return null;
        // Normalise: upper-case, collapse whitespace/hyphens to underscore
        String normalised = raw.trim()
                               .toUpperCase()
                               .replaceAll("[\\s\\-]+", "_");
        try {
            return DonationType.valueOf(normalised);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
