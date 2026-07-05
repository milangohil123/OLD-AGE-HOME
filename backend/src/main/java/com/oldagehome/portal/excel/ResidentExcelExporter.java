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
     *   0: Resident ID          1: Full Name           2: Gender
     *   3: Date of Birth        4: Mobile              5: Guardian Name
     *   6: Guardian Phone       7: Guardian Email      8: Guardian Address
     *   9: Room Number         10: Medical Prescription 11: Occupation
     *  12: Disability          13: Aadhaar Number      14: Age
     *  15: Blood Group         16: Email               17: Address
     *  18: Joining Date        19: Status              20: Medical Notes
     */
    public static byte[] exportResidents(List<Resident> residents) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Residents");

        // Headers
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "Resident ID", "Full Name", "Gender", "Date of Birth", "Mobile",
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
            row.createCell(0).setCellValue(r.getResidentId() != null ? r.getResidentId() : "");
            row.createCell(1).setCellValue(r.getFullName() != null ? r.getFullName() : "");
            row.createCell(2).setCellValue(r.getGender() != null ? r.getGender() : "");
            row.createCell(3).setCellValue(r.getDateOfBirth() != null ? r.getDateOfBirth().format(dtf) : "");
            row.createCell(4).setCellValue(r.getMobile() != null ? r.getMobile() : "");
            row.createCell(5).setCellValue(r.getGuardianName() != null ? r.getGuardianName() : "");
            row.createCell(6).setCellValue(r.getGuardianPhone() != null ? r.getGuardianPhone() : "");
            row.createCell(7).setCellValue(r.getGuardianEmail() != null ? r.getGuardianEmail() : "");
            row.createCell(8).setCellValue(r.getGuardianAddress() != null ? r.getGuardianAddress() : "");
            row.createCell(9).setCellValue(r.getRoomNumber() != null ? r.getRoomNumber() : "");
            row.createCell(10).setCellValue(r.getMedicalPrescription() != null ? r.getMedicalPrescription() : "");
            row.createCell(11).setCellValue(r.getOccupation() != null ? r.getOccupation() : "");
            row.createCell(12).setCellValue(r.getDisability() != null ? r.getDisability() : "");
            row.createCell(13).setCellValue(r.getAadhaarNumber() != null ? r.getAadhaarNumber() : "");
            row.createCell(14).setCellValue(r.getAge() != null ? r.getAge() : 0);
            row.createCell(15).setCellValue(r.getBloodGroup() != null ? r.getBloodGroup() : "");
            row.createCell(16).setCellValue(r.getEmail() != null ? r.getEmail() : "");
            row.createCell(17).setCellValue(r.getAddress() != null ? r.getAddress() : "");
            row.createCell(18).setCellValue(r.getJoiningDate() != null ? r.getJoiningDate().format(dtf) : "");
            row.createCell(19).setCellValue(r.getStatus() != null ? r.getStatus().name() : "");
            row.createCell(20).setCellValue(r.getMedicalNotes() != null ? r.getMedicalNotes() : "");
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
