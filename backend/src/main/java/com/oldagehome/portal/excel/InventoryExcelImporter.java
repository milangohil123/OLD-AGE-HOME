package com.oldagehome.portal.excel;

import com.oldagehome.portal.inventory.MedicineCategory;
import com.oldagehome.portal.dto.InventoryImportDTO;
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
 * Parses an uploaded Excel file (.xlsx) containing inventory records.
 *
 * Expected column order:
 *  0  - Medicine Code
 *  1  - Medicine Name
 *  2  - Category        (TABLET / SYRUP / INJECTION / CAPSULE / OINTMENT / DROPS / INHALER / CREAM / POWDER / OTHER)
 *  3  - Manufacturer
 *  4  - Supplier
 *  5  - Batch Number
 *  6  - Purchase Date   (dd-MM-yyyy or numeric Excel date)
 *  7  - Expiry Date     (dd-MM-yyyy or numeric Excel date)
 *  8  - Quantity
 *  9  - Minimum Stock
 *  10 - Unit Price
 *  11 - Rack Location
 *  12 - Notes
 */
public class InventoryExcelImporter {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public static List<InventoryImportDTO> importInventory(InputStream is) throws Exception {
        List<InventoryImportDTO> dtos = new ArrayList<>();
        Workbook workbook = new XSSFWorkbook(is);
        Sheet sheet = workbook.getSheetAt(0);
        DataFormatter dataFormatter = new DataFormatter();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            InventoryImportDTO dto = new InventoryImportDTO();
            dto.setRowNum(i + 1);
            StringBuilder errors = new StringBuilder();

            // --- Column 0: Medicine Code ---
            String medicineCode = getCellValue(row.getCell(0), dataFormatter).trim();
            dto.setMedicineCode(medicineCode);
            if (medicineCode.isEmpty()) {
                dto.setValid(false);
                errors.append("Medicine Code is required. ");
            }

            // --- Column 1: Medicine Name ---
            String medicineName = getCellValue(row.getCell(1), dataFormatter).trim();
            dto.setMedicineName(medicineName);
            if (medicineName.isEmpty()) {
                dto.setValid(false);
                errors.append("Medicine Name is required. ");
            }

            // --- Column 2: Category ---
            String categoryStr = getCellValue(row.getCell(2), dataFormatter).trim().toUpperCase();
            if (!categoryStr.isEmpty()) {
                try {
                    dto.setCategory(MedicineCategory.valueOf(categoryStr));
                } catch (IllegalArgumentException e) {
                    dto.setValid(false);
                    errors.append("Invalid Category: ").append(categoryStr).append(". ");
                }
            } else {
                dto.setValid(false);
                errors.append("Category is required. ");
            }

            // --- Column 3: Manufacturer ---
            dto.setManufacturer(getCellValue(row.getCell(3), dataFormatter).trim());

            // --- Column 4: Supplier ---
            dto.setSupplier(getCellValue(row.getCell(4), dataFormatter).trim());

            // --- Column 5: Batch Number ---
            dto.setBatchNumber(getCellValue(row.getCell(5), dataFormatter).trim());

            // --- Column 6: Purchase Date ---
            Cell purchaseDateCell = row.getCell(6);
            if (purchaseDateCell != null) {
                if (purchaseDateCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(purchaseDateCell)) {
                    Date date = purchaseDateCell.getDateCellValue();
                    dto.setPurchaseDate(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                } else {
                    String dateStr = getCellValue(purchaseDateCell, dataFormatter).trim();
                    dto.setPurchaseDateString(dateStr);
                    if (!dateStr.isEmpty()) {
                        try {
                            dto.setPurchaseDate(LocalDate.parse(dateStr, DATE_FMT));
                        } catch (DateTimeParseException e) {
                            dto.setValid(false);
                            errors.append("Invalid Purchase Date format. Use dd-MM-yyyy. ");
                        }
                    }
                }
            }
            if (dto.getPurchaseDate() == null && dto.isValid()) {
                dto.setPurchaseDate(LocalDate.now());
            }

            // --- Column 7: Expiry Date ---
            Cell expiryDateCell = row.getCell(7);
            if (expiryDateCell != null) {
                if (expiryDateCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(expiryDateCell)) {
                    Date date = expiryDateCell.getDateCellValue();
                    dto.setExpiryDate(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                } else {
                    String dateStr = getCellValue(expiryDateCell, dataFormatter).trim();
                    dto.setExpiryDateString(dateStr);
                    if (!dateStr.isEmpty()) {
                        try {
                            dto.setExpiryDate(LocalDate.parse(dateStr, DATE_FMT));
                        } catch (DateTimeParseException e) {
                            dto.setValid(false);
                            errors.append("Invalid Expiry Date format. Use dd-MM-yyyy. ");
                        }
                    }
                }
            }
            if (dto.getExpiryDate() == null) {
                dto.setValid(false);
                errors.append("Expiry Date is required. ");
            }

            // Date cross-validation
            if (dto.getPurchaseDate() != null && dto.getExpiryDate() != null
                    && dto.getPurchaseDate().isAfter(dto.getExpiryDate())) {
                dto.setValid(false);
                errors.append("Purchase Date cannot be after Expiry Date. ");
            }

            // --- Column 8: Quantity ---
            Cell qtyCell = row.getCell(8);
            if (qtyCell != null && qtyCell.getCellType() == CellType.NUMERIC) {
                int qty = (int) qtyCell.getNumericCellValue();
                if (qty < 0) {
                    dto.setValid(false);
                    errors.append("Quantity cannot be negative. ");
                } else {
                    dto.setQuantity(qty);
                }
            } else {
                String qtyStr = getCellValue(qtyCell, dataFormatter).trim();
                if (!qtyStr.isEmpty()) {
                    try {
                        int qty = Integer.parseInt(qtyStr);
                        if (qty < 0) {
                            dto.setValid(false);
                            errors.append("Quantity cannot be negative. ");
                        } else {
                            dto.setQuantity(qty);
                        }
                    } catch (NumberFormatException e) {
                        dto.setValid(false);
                        errors.append("Invalid Quantity format. ");
                    }
                } else {
                    dto.setQuantity(0);
                }
            }

            // --- Column 9: Minimum Stock ---
            Cell minStockCell = row.getCell(9);
            if (minStockCell != null && minStockCell.getCellType() == CellType.NUMERIC) {
                dto.setMinimumStock((int) minStockCell.getNumericCellValue());
            } else {
                String minStr = getCellValue(minStockCell, dataFormatter).trim();
                if (!minStr.isEmpty()) {
                    try {
                        dto.setMinimumStock(Integer.parseInt(minStr));
                    } catch (NumberFormatException e) {
                        dto.setMinimumStock(10); // default
                    }
                } else {
                    dto.setMinimumStock(10);
                }
            }

            // --- Column 10: Unit Price ---
            Cell priceCell = row.getCell(10);
            if (priceCell != null && priceCell.getCellType() == CellType.NUMERIC) {
                double priceVal = priceCell.getNumericCellValue();
                if (priceVal < 0) {
                    dto.setValid(false);
                    errors.append("Unit Price cannot be negative. ");
                } else {
                    dto.setUnitPrice(BigDecimal.valueOf(priceVal));
                }
            } else {
                String priceStr = getCellValue(priceCell, dataFormatter).trim();
                if (!priceStr.isEmpty()) {
                    try {
                        BigDecimal price = new BigDecimal(priceStr);
                        if (price.compareTo(BigDecimal.ZERO) < 0) {
                            dto.setValid(false);
                            errors.append("Unit Price cannot be negative. ");
                        } else {
                            dto.setUnitPrice(price);
                        }
                    } catch (NumberFormatException e) {
                        dto.setValid(false);
                        errors.append("Invalid Unit Price format. ");
                    }
                } else {
                    dto.setUnitPrice(BigDecimal.ZERO);
                }
            }

            // --- Column 11: Rack Location ---
            dto.setRackLocation(getCellValue(row.getCell(11), dataFormatter).trim());

            // --- Column 12: Notes ---
            dto.setNotes(getCellValue(row.getCell(12), dataFormatter).trim());

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
}
