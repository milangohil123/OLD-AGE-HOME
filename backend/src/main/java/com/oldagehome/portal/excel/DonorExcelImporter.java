package com.oldagehome.portal.excel;

import com.oldagehome.portal.donor.DonorStatus;
import com.oldagehome.portal.donor.DonationType;
import com.oldagehome.portal.donor.DonationFrequency;
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
 *  0 - Donor Name
 *  1 - Mobile
 *  2 - Email
 *  3 - Address
 *  4 - Donation Frequency (ONE_TIME / MONTHLY / YEARLY)
 *  5 - Donation Type      (FOOD / MEDICINE / CASH / CHEQUE / UPI / GOODS / OTHER)
 *  6 - Payment Method
 *  7 - Transaction ID
 *  8 - Donation Date      (dd-MM-yyyy or numeric Excel date)
 *  9 - Medicine Name      (Only for MEDICINE type)
 *  10 - Price             (Only for MEDICINE type)
 *  11 - Expiry Date       (Only for MEDICINE type)
 *  12 - Food Name         (Only for FOOD type)
 *  13 - Quantity          (Only for FOOD type)
 *  14 - Donation Amount   (Only for CASH / UPI / CHEQUE / GOODS / OTHER types)
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

            // --- Column 0: Donor Name ---
            String fullName = getCellValue(row.getCell(0), dataFormatter).trim();
            dto.setFullName(fullName);
            if (fullName.isEmpty()) {
                dto.setValid(false);
                errors.append("Donor Name is required. ");
            }

            // --- Column 1: Mobile ---
            dto.setMobile(getCellValue(row.getCell(1), dataFormatter).trim());

            // --- Column 2: Email ---
            dto.setEmail(getCellValue(row.getCell(2), dataFormatter).trim());

            // --- Column 3: Address ---
            dto.setAddress(getCellValue(row.getCell(3), dataFormatter).trim());

            // Default gender to MALE/OTHER because it is required in DB but not in excel columns
            dto.setGender("OTHER");
            dto.setStatus(DonorStatus.ACTIVE);

            // --- Column 4: Donation Frequency ---
            String freqStr = getCellValue(row.getCell(4), dataFormatter).trim();
            if (!freqStr.isEmpty()) {
                DonationFrequency parsedFreq = parseDonationFrequency(freqStr);
                if (parsedFreq == null) {
                    dto.setValid(false);
                    errors.append("Invalid Donation Frequency: '").append(freqStr)
                          .append("'. Supported values: One Time, Monthly, Yearly. ");
                } else {
                    dto.setDonationFrequency(parsedFreq);
                }
            } else {
                dto.setValid(false);
                errors.append("Donation Frequency is required. ");
            }

            // --- Column 5: Donation Type ---
            String donationTypeStr = getCellValue(row.getCell(5), dataFormatter).trim();
            DonationType parsedType = null;
            if (!donationTypeStr.isEmpty()) {
                parsedType = parseDonationType(donationTypeStr);
                if (parsedType == null) {
                    dto.setValid(false);
                    errors.append("Invalid Donation Type: '").append(donationTypeStr)
                          .append("'. Supported values: FOOD, MEDICINE, CASH, CHEQUE, UPI, GOODS, OTHER. ");
                } else {
                    dto.setDonationType(parsedType);
                }
            } else {
                dto.setValid(false);
                errors.append("Donation Type is required. ");
            }

            // --- Column 6: Payment Method ---
            dto.setPaymentMethod(getCellValue(row.getCell(6), dataFormatter).trim());

            // --- Column 7: Transaction ID ---
            dto.setTransactionId(getCellValue(row.getCell(7), dataFormatter).trim());

            // --- Column 8: Donation Date ---
            Cell dateCell = row.getCell(8);
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

            if (parsedType != null) {
                if (parsedType == DonationType.MEDICINE) {
                    // --- Column 9: Medicine Name ---
                    String medName = getCellValue(row.getCell(9), dataFormatter).trim();
                    dto.setMedicineName(medName);
                    if (medName.isEmpty()) {
                        dto.setValid(false);
                        errors.append("Medicine Name is required for Medicine donation. ");
                    }

                    // --- Column 10: Price ---
                    Cell priceCell = row.getCell(10);
                    if (priceCell != null && priceCell.getCellType() == CellType.NUMERIC) {
                        dto.setMedicinePrice(BigDecimal.valueOf(priceCell.getNumericCellValue()));
                    } else {
                        String priceStr = getCellValue(priceCell, dataFormatter).trim();
                        if (!priceStr.isEmpty()) {
                            try {
                                dto.setMedicinePrice(new BigDecimal(priceStr));
                            } catch (NumberFormatException e) {
                                dto.setValid(false);
                                errors.append("Invalid medicine price format. ");
                            }
                        } else {
                            dto.setValid(false);
                            errors.append("Medicine Price is required for Medicine donation. ");
                        }
                    }

                    // --- Column 11: Expiry Date ---
                    Cell expCell = row.getCell(11);
                    if (expCell != null) {
                        if (expCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(expCell)) {
                            Date date = expCell.getDateCellValue();
                            dto.setMedicineExpiryDate(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                        } else {
                            String expStr = getCellValue(expCell, dataFormatter).trim();
                            if (!expStr.isEmpty()) {
                                try {
                                    dto.setMedicineExpiryDate(LocalDate.parse(expStr, DATE_FMT));
                                } catch (DateTimeParseException e) {
                                    dto.setValid(false);
                                    errors.append("Invalid expiry date format. Use dd-MM-yyyy. ");
                                }
                            } else {
                                dto.setValid(false);
                                errors.append("Medicine Expiry Date is required. ");
                            }
                        }
                    } else {
                        dto.setValid(false);
                        errors.append("Medicine Expiry Date is required. ");
                    }

                } else if (parsedType == DonationType.FOOD) {
                    // --- Column 12: Food Name ---
                    String foodName = getCellValue(row.getCell(12), dataFormatter).trim();
                    dto.setFoodName(foodName);
                    if (foodName.isEmpty()) {
                        dto.setValid(false);
                        errors.append("Food Name is required for Food donation. ");
                    }

                    // --- Column 13: Quantity ---
                    String qty = getCellValue(row.getCell(13), dataFormatter).trim();
                    dto.setFoodQuantity(qty);
                    if (qty.isEmpty()) {
                        dto.setValid(false);
                        errors.append("Food Quantity is required for Food donation. ");
                    }

                } else {
                    // Cash/UPI/Cheque/Goods/Other
                    // --- Column 14: Donation Amount ---
                    Cell amtCell = row.getCell(14);
                    if (amtCell != null && amtCell.getCellType() == CellType.NUMERIC) {
                        dto.setDonationAmount(BigDecimal.valueOf(amtCell.getNumericCellValue()));
                    } else {
                        String amtStr = getCellValue(amtCell, dataFormatter).trim();
                        if (!amtStr.isEmpty()) {
                            try {
                                dto.setDonationAmount(new BigDecimal(amtStr));
                            } catch (NumberFormatException e) {
                                dto.setValid(false);
                                errors.append("Invalid donation amount format. ");
                            }
                        } else {
                            if (parsedType == DonationType.CASH || parsedType == DonationType.UPI || parsedType == DonationType.CHEQUE) {
                                dto.setValid(false);
                                errors.append("Donation Amount is required. ");
                            }
                        }
                    }
                }
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

    private static DonationFrequency parseDonationFrequency(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String normalised = raw.trim().toUpperCase().replaceAll("[\\s\\-]+", "_");
        try {
            return DonationFrequency.valueOf(normalised);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static DonationType parseDonationType(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String normalised = raw.trim().toUpperCase().replaceAll("[\\s\\-]+", "_");
        try {
            return DonationType.valueOf(normalised);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
