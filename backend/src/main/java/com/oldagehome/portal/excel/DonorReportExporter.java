package com.oldagehome.portal.excel;

import com.oldagehome.portal.donor.Donor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DonorReportExporter {

    public static byte[] exportExcel(List<Donor> donors, String title) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Donors Report");

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

        // Donor ID removed from headers
        String[] headers = {
            "Full Name", "Gender", "Age", "Mobile", "Email", "Donation Type", "Status", "City", "State"
        };

        Row headerRow = sheet.createRow(2);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 3;
        for (Donor d : donors) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(d.getFullName() != null ? d.getFullName() : "");
            row.createCell(1).setCellValue(d.getGender() != null ? d.getGender() : "");
            row.createCell(2).setCellValue(d.getAge() != null ? d.getAge() : 0);
            row.createCell(3).setCellValue(d.getMobile() != null ? d.getMobile() : "");
            row.createCell(4).setCellValue(d.getEmail() != null ? d.getEmail() : "");
            row.createCell(5).setCellValue(d.getDonationType() != null ? d.getDonationType().name() : "");
            row.createCell(6).setCellValue(d.getStatus() != null ? d.getStatus().name() : "");
            row.createCell(7).setCellValue(d.getCity() != null ? d.getCity() : "");
            row.createCell(8).setCellValue(d.getState() != null ? d.getState() : "");
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
