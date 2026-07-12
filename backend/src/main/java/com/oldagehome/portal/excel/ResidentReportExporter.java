package com.oldagehome.portal.excel;

import com.oldagehome.portal.resident.Resident;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ResidentReportExporter {

    public static byte[] exportExcel(List<Resident> residents, String title) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Residents Report");

        // Heading Row
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title.toUpperCase());
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);
        titleCell.setCellStyle(titleStyle);

        // Header style
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
            "Full Name", "Gender", "Age", "Blood Group", "Mobile", "Email", "Room Number", "Joining Date", "Status", "Guardian Name", "Guardian Phone"
        };

        Row headerRow = sheet.createRow(2);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        int rowIdx = 3;
        for (Resident r : residents) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(r.getFullName() != null ? r.getFullName() : "");
            row.createCell(1).setCellValue(r.getGender() != null ? r.getGender() : "");
            row.createCell(2).setCellValue(r.getAge() != null ? r.getAge() : 0);
            row.createCell(3).setCellValue(r.getBloodGroup() != null ? r.getBloodGroup() : "");
            row.createCell(4).setCellValue(r.getMobile() != null ? r.getMobile() : "");
            row.createCell(5).setCellValue(r.getEmail() != null ? r.getEmail() : "");
            row.createCell(6).setCellValue(r.getRoomNumber() != null ? r.getRoomNumber() : "");
            row.createCell(7).setCellValue(r.getJoiningDate() != null ? r.getJoiningDate().format(dtf) : "");
            row.createCell(8).setCellValue(r.getStatus() != null ? r.getStatus().name() : "");
            row.createCell(9).setCellValue(r.getGuardianName() != null ? r.getGuardianName() : "");
            row.createCell(10).setCellValue(r.getGuardianPhone() != null ? r.getGuardianPhone() : "");
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
