package com.oldagehome.portal.excel;

import com.oldagehome.portal.donor.Donor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Exports donor records to an Excel (.xlsx) file.
 *
 * Exported columns (Donor ID removed):
 *  Full Name | Gender | Age | Mobile | Email |
 *  Donation Type | Amount | Donation Date | Payment Method |
 *  Transaction ID | Status | City | State | Remarks
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
            "Full Name", "Gender", "Age", "Mobile", "Email",
            "Donation Type", "Amount", "Donation Date", "Payment Method",
            "Transaction ID", "Status", "City", "State", "Remarks"
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
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(d.getFullName() != null ? d.getFullName() : "");
            row.createCell(1).setCellValue(d.getGender() != null ? d.getGender() : "");
            row.createCell(2).setCellValue(d.getAge() != null ? d.getAge() : 0);
            row.createCell(3).setCellValue(d.getMobile() != null ? d.getMobile() : "");
            row.createCell(4).setCellValue(d.getEmail() != null ? d.getEmail() : "");
            row.createCell(5).setCellValue(d.getDonationType() != null ? d.getDonationType().name() : "");
            row.createCell(6).setCellValue(d.getDonationAmount() != null ? d.getDonationAmount().doubleValue() : 0.0);
            row.createCell(7).setCellValue(d.getDonationDate() != null ? d.getDonationDate().format(dtf) : "");
            row.createCell(8).setCellValue(d.getPaymentMethod() != null ? d.getPaymentMethod() : "");
            row.createCell(9).setCellValue(d.getTransactionId() != null ? d.getTransactionId() : "");
            row.createCell(10).setCellValue(d.getStatus() != null ? d.getStatus().name() : "");
            row.createCell(11).setCellValue(d.getCity() != null ? d.getCity() : "");
            row.createCell(12).setCellValue(d.getState() != null ? d.getState() : "");
            row.createCell(13).setCellValue(d.getRemarks() != null ? d.getRemarks() : "");
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
}
