package com.oldagehome.portal.excel;

import com.oldagehome.portal.inventory.Inventory;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class InventoryReportExporter {

    public static byte[] exportExcel(List<Inventory> items, String title) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Inventory Report");

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
            "Medicine Code", "Medicine Name", "Category", "Manufacturer", "Supplier", "Batch Number", "Purchase Date", "Expiry Date", "Quantity", "Min Stock", "Unit Price (₹)", "Rack Location", "Status"
        };

        Row headerRow = sheet.createRow(2);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        int rowIdx = 3;
        for (Inventory item : items) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(item.getMedicineCode() != null ? item.getMedicineCode() : "");
            row.createCell(1).setCellValue(item.getMedicineName() != null ? item.getMedicineName() : "");
            row.createCell(2).setCellValue(item.getCategory() != null ? item.getCategory().name() : "");
            row.createCell(3).setCellValue(item.getManufacturer() != null ? item.getManufacturer() : "");
            row.createCell(4).setCellValue(item.getSupplier() != null ? item.getSupplier() : "");
            row.createCell(5).setCellValue(item.getBatchNumber() != null ? item.getBatchNumber() : "");
            row.createCell(6).setCellValue(item.getPurchaseDate() != null ? item.getPurchaseDate().format(dtf) : "");
            row.createCell(7).setCellValue(item.getExpiryDate() != null ? item.getExpiryDate().format(dtf) : "");
            row.createCell(8).setCellValue(item.getQuantity() != null ? item.getQuantity() : 0);
            row.createCell(9).setCellValue(item.getMinimumStock() != null ? item.getMinimumStock() : 10);
            row.createCell(10).setCellValue(item.getUnitPrice() != null ? item.getUnitPrice().doubleValue() : 0.0);
            row.createCell(11).setCellValue(item.getRackLocation() != null ? item.getRackLocation() : "");
            row.createCell(12).setCellValue(item.getStatus() != null ? item.getStatus().name() : "");
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
