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
     * This also serves as the sample import template — columns match the importer exactly.
     * Column order:
     *   0: Full Name           1: Gender
     *   2: Date of Birth       3: Mobile              4: Guardian Name
     *   5: Guardian Phone      6: Guardian Email      7: Guardian Address
     *   8: Room Number         9: Medical Prescription 10: Occupation
     *  11: Disability         12: Aadhaar Number      13: Age
     *  14: Blood Group        15: Email               16: Address
     *  17: Joining Date       18: Status              19: Medical Notes
     */
    public static byte[] exportResidents(List<Resident> residents) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Residents");

        // Headers
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "Full Name", "Gender", "Date of Birth", "Mobile",
            "Guardian Name", "Guardian Phone", "Guardian Email", "Guardian Address",
            "Room Number", "Medical Prescription", "Occupation", "Disability",
            "Aadhaar Number", "Age", "Blood Group", "Email", "Address",
            "Joining Date", "Status", "Medical Notes"
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
            row.createCell(0).setCellValue(r.getFullName() != null ? r.getFullName() : "");
            row.createCell(1).setCellValue(r.getGender() != null ? r.getGender() : "");
            row.createCell(2).setCellValue(r.getDateOfBirth() != null ? r.getDateOfBirth().format(dtf) : "");
            row.createCell(3).setCellValue(r.getMobile() != null ? r.getMobile() : "");
            row.createCell(4).setCellValue(r.getGuardianName() != null ? r.getGuardianName() : "");
            row.createCell(5).setCellValue(r.getGuardianPhone() != null ? r.getGuardianPhone() : "");
            row.createCell(6).setCellValue(r.getGuardianEmail() != null ? r.getGuardianEmail() : "");
            row.createCell(7).setCellValue(r.getGuardianAddress() != null ? r.getGuardianAddress() : "");
            row.createCell(8).setCellValue(r.getRoomNumber() != null ? r.getRoomNumber() : "");
            row.createCell(9).setCellValue(r.getMedicalPrescription() != null ? r.getMedicalPrescription() : "");
            row.createCell(10).setCellValue(r.getOccupation() != null ? r.getOccupation() : "");
            row.createCell(11).setCellValue(r.getDisability() != null ? r.getDisability() : "");
            row.createCell(12).setCellValue(r.getAadhaarNumber() != null ? r.getAadhaarNumber() : "");
            row.createCell(13).setCellValue(r.getAge() != null ? r.getAge() : 0);
            row.createCell(14).setCellValue(r.getBloodGroup() != null ? r.getBloodGroup() : "");
            row.createCell(15).setCellValue(r.getEmail() != null ? r.getEmail() : "");
            row.createCell(16).setCellValue(r.getAddress() != null ? r.getAddress() : "");
            row.createCell(17).setCellValue(r.getJoiningDate() != null ? r.getJoiningDate().format(dtf) : "");
            row.createCell(18).setCellValue(r.getStatus() != null ? r.getStatus().name() : "");
            row.createCell(19).setCellValue(r.getMedicalNotes() != null ? r.getMedicalNotes() : "");
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
