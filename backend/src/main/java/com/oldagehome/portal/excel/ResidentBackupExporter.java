package com.oldagehome.portal.excel;

import com.oldagehome.portal.resident.Resident;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ResidentBackupExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static byte[] exportResidentsForBackup(List<Resident> residents) throws IOException {
        String[] columns = {"Resident ID", "Name", "Gender", "DOB", "Mobile", "Guardian", "Guardian Phone"};

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Residents Backup");

            // Header styling
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.BLACK.getIndex());
            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerCellStyle.setBorderBottom(BorderStyle.THIN);

            // Header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerCellStyle);
            }

            // Data rows
            int rowIdx = 1;
            for (Resident resident : residents) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(resident.getId() != null ? String.valueOf(resident.getId()) : "");
                row.createCell(1).setCellValue(resident.getFullName() != null ? resident.getFullName() : "");
                row.createCell(2).setCellValue(resident.getGender() != null ? resident.getGender() : "");
                row.createCell(3).setCellValue(resident.getDateOfBirth() != null ? resident.getDateOfBirth().format(DATE_FORMATTER) : "");
                row.createCell(4).setCellValue(resident.getMobile() != null ? resident.getMobile() : "");
                row.createCell(5).setCellValue(resident.getGuardianName() != null ? resident.getGuardianName() : "");
                row.createCell(6).setCellValue(resident.getGuardianPhone() != null ? resident.getGuardianPhone() : "");
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
