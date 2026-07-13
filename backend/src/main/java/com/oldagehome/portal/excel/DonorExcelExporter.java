package com.oldagehome.portal.excel;

import com.oldagehome.portal.donor.Donor;
import com.oldagehome.portal.donor.FoodDonationItem;
import com.oldagehome.portal.donor.MedicineDonationItem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Exports donor records to an Excel (.xlsx) file matching the import structure.
 *
 * Columns:
 *  Donor Name | Mobile | Email | Address | Donation Frequency | Donation Type |
 *  Payment Method | Transaction ID | Donation Date |
 *  Medicine Name | Price | Expiry Date | Food Name | Quantity | Donation Amount
 */
public class DonorExcelExporter {

    public static byte[] exportDonors(List<Donor> donors) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Donors");

        // ── Header style ──────────────────────────────────────────────────────
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 11);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);

        // ── Header row ────────────────────────────────────────────────────────
        String[] headers = {
            "Donor Name", "Mobile", "Email", "Address", "Donation Frequency", "Donation Type",
            "Payment Method", "Transaction ID", "Donation Date",
            "Medicine Name", "Price", "Expiry Date", "Food Name", "Quantity", "Donation Amount"
        };

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        // ── Data rows ─────────────────────────────────────────────────────────
        int rowIdx = 1;
        for (Donor d : donors) {
            if (d.getDonationType() == com.oldagehome.portal.donor.DonationType.MEDICINE && d.getMedicineItems() != null && !d.getMedicineItems().isEmpty()) {
                for (MedicineDonationItem item : d.getMedicineItems()) {
                    Row row = sheet.createRow(rowIdx++);
                    writeCommonColumns(row, d, dtf);
                    row.createCell(9).setCellValue(item.getMedicineName() != null ? item.getMedicineName() : "");
                    row.createCell(10).setCellValue(item.getPrice() != null ? item.getPrice().doubleValue() : 0.0);
                    row.createCell(11).setCellValue(item.getExpiryDate() != null ? item.getExpiryDate().format(dtf) : "");
                }
            } else if (d.getDonationType() == com.oldagehome.portal.donor.DonationType.FOOD && d.getFoodItems() != null && !d.getFoodItems().isEmpty()) {
                for (FoodDonationItem item : d.getFoodItems()) {
                    Row row = sheet.createRow(rowIdx++);
                    writeCommonColumns(row, d, dtf);
                    row.createCell(12).setCellValue(item.getFoodName() != null ? item.getFoodName() : "");
                    row.createCell(13).setCellValue(item.getQuantity() != null ? item.getQuantity() : "");
                }
            } else {
                Row row = sheet.createRow(rowIdx++);
                writeCommonColumns(row, d, dtf);
                if (d.getDonationAmount() != null) {
                    row.createCell(14).setCellValue(d.getDonationAmount().doubleValue());
                } else {
                    row.createCell(14).setCellValue(0.0);
                }
            }
        }

        // ── Auto-size all columns ─────────────────────────────────────────────
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();
        return bos.toByteArray();
    }

    private static void writeCommonColumns(Row row, Donor d, DateTimeFormatter dtf) {
        row.createCell(0).setCellValue(d.getFullName() != null ? d.getFullName() : "");
        row.createCell(1).setCellValue(d.getMobile() != null ? d.getMobile() : "");
        row.createCell(2).setCellValue(d.getEmail() != null ? d.getEmail() : "");
        row.createCell(3).setCellValue(d.getAddress() != null ? d.getAddress() : "");
        row.createCell(4).setCellValue(d.getDonationFrequency() != null ? d.getDonationFrequency().name() : "");
        row.createCell(5).setCellValue(d.getDonationType() != null ? d.getDonationType().name() : "");
        row.createCell(6).setCellValue(d.getPaymentMethod() != null ? d.getPaymentMethod() : "");
        row.createCell(7).setCellValue(d.getTransactionId() != null ? d.getTransactionId() : "");
        row.createCell(8).setCellValue(d.getDonationDate() != null ? d.getDonationDate().format(dtf) : "");
    }
}
