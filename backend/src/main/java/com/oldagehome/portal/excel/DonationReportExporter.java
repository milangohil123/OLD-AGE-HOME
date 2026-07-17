package com.oldagehome.portal.excel;

import com.oldagehome.portal.donor.Donor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DonationReportExporter {

    public static byte[] exportExcel(List<Donor> donations, String title) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Donations Report");

        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title.toUpperCase());
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);
        titleCell.setCellStyle(titleStyle);

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 11);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);

        String[] headers = {
            "Donor Name", "Donation Frequency", "Donation Type", "Amount (₹)", "Donation Date", "Payment Method", "Transaction ID", "Remarks"
        };

        Row headerRow = sheet.createRow(2);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        int rowIdx = 3;
        for (Donor d : donations) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(d.getFullName() != null ? d.getFullName() : "");
            row.createCell(1).setCellValue(d.getDonationFrequency() != null ? d.getDonationFrequency().getDisplayName() : "");
            row.createCell(2).setCellValue(d.getDonationType() != null ? d.getDonationType().getDisplayName() : "");
            row.createCell(3).setCellValue(d.getDonationAmount() != null ? d.getDonationAmount().doubleValue() : 0.0);
            row.createCell(4).setCellValue(d.getDonationDate() != null ? d.getDonationDate().format(dtf) : "");
            row.createCell(5).setCellValue(d.getPaymentMethod() != null ? d.getPaymentMethod() : "");
            row.createCell(6).setCellValue(d.getTransactionId() != null ? d.getTransactionId() : "");
            row.createCell(7).setCellValue(d.getRemarks() != null ? d.getRemarks() : "");
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();
        return bos.toByteArray();
    }
}
