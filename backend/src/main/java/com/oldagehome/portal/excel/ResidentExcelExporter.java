package com.oldagehome.portal.excel;

import com.oldagehome.portal.resident.Resident;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ResidentExcelExporter {

    /**
     * Exports a list of Resident records into a binary Excel stream (.xlsx).
     */
    public static byte[] exportResidents(List<Resident> residents) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Residents");

        // Headers
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "Resident ID", "Full Name", "Gender", "Date of Birth", "Age", 
            "Blood Group", "Mobile", "Email", "Address", "Joining Date", 
            "Status", "Guardian Name", "Guardian Phone", "Medical Notes"
        };
        
        // Header styling
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Format dates consistently
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        // Populate cells
        int rowIdx = 1;
        for (Resident r : residents) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(r.getResidentId() != null ? r.getResidentId() : "");
            row.createCell(1).setCellValue(r.getFullName() != null ? r.getFullName() : "");
            row.createCell(2).setCellValue(r.getGender() != null ? r.getGender() : "");
            row.createCell(3).setCellValue(r.getDateOfBirth() != null ? r.getDateOfBirth().format(dtf) : "");
            row.createCell(4).setCellValue(r.getAge() != null ? r.getAge() : 0);
            row.createCell(5).setCellValue(r.getBloodGroup() != null ? r.getBloodGroup() : "");
            row.createCell(6).setCellValue(r.getMobile() != null ? r.getMobile() : "");
            row.createCell(7).setCellValue(r.getEmail() != null ? r.getEmail() : "");
            row.createCell(8).setCellValue(r.getAddress() != null ? r.getAddress() : "");
            row.createCell(9).setCellValue(r.getJoiningDate() != null ? r.getJoiningDate().format(dtf) : "");
            row.createCell(10).setCellValue(r.getStatus() != null ? r.getStatus().name() : "");
            row.createCell(11).setCellValue(r.getGuardianName() != null ? r.getGuardianName() : "");
            row.createCell(12).setCellValue(r.getGuardianPhone() != null ? r.getGuardianPhone() : "");
            row.createCell(13).setCellValue(r.getMedicalNotes() != null ? r.getMedicalNotes() : "");
        }

        // Auto-fit columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();
        return bos.toByteArray();
    }
}
