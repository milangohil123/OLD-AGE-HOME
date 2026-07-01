package com.oldagehome.portal.audit;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.apache.poi.ss.usermodel.*;
// import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Autowired
    public AuditServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public AuditLog saveAuditLog(AuditLog auditLog) {
        return auditLogRepository.save(auditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLog getAuditLogById(Long id) {
        return auditLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Audit log entry not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return auditLogRepository.searchLogs(keyword.trim(), pageable);
        }
        return auditLogRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> filterLogs(
            String username,
            String module,
            String action,
            Boolean success,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {
        
        AuditModule mod = (module != null && !module.isEmpty() && !module.equalsIgnoreCase("ALL")) ? AuditModule.valueOf(module.toUpperCase()) : null;
        AuditAction act = (action != null && !action.isEmpty() && !action.equalsIgnoreCase("ALL")) ? AuditAction.valueOf(action.toUpperCase()) : null;
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : null;
        String user = (username != null && !username.isEmpty()) ? username.trim() : null;

        return auditLogRepository.filterLogs(user, mod, act, success, start, end, pageable);
    }

    @Override
    public void deleteOldLogs(LocalDateTime beforeDate) {
        // Find logs older than beforeDate and delete them
        List<AuditLog> oldLogs = auditLogRepository.findAll().stream()
            .filter(log -> log.getTimestamp().isBefore(beforeDate))
            .toList();
        auditLogRepository.deleteAll(oldLogs);
    }

    @Override
    public byte[] exportLogs(List<AuditLog> logs, String format) throws IOException {
        if ("EXCEL".equalsIgnoreCase(format)) {
            return exportToExcel(logs);
        } else if ("PDF".equalsIgnoreCase(format)) {
            return exportToPdf(logs);
        } else {
            return exportToCsv(logs);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countTodayActivities() {
        return auditLogRepository.countByTimestampAfter(LocalDate.now().atStartOfDay());
    }

    @Override
    @Transactional(readOnly = true)
    public long countFailedActivities() {
        return auditLogRepository.countByTimestampAfterAndSuccess(LocalDate.now().atStartOfDay(), false);
    }

    @Override
    @Transactional(readOnly = true)
    public long countSuccessfulActivities() {
        return auditLogRepository.countByTimestampAfterAndSuccess(LocalDate.now().atStartOfDay(), true);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalRecords() {
        return auditLogRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDateTime getLatestLogin() {
        return auditLogRepository.findFirstByActionOrderByTimestampDesc(AuditAction.LOGIN)
                .map(AuditLog::getTimestamp)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDateTime getLatestSystemUpdate() {
        return auditLogRepository.findFirstByActionOrderByTimestampDesc(AuditAction.UPDATE)
                .map(AuditLog::getTimestamp)
                .orElse(null);
    }

    @Override
    public void logActivity(
            AuditModule module,
            AuditAction action,
            String description,
            String entityName,
            Long entityId,
            boolean success,
            String errorMessage) {

        String username = "system";
        String role = "SYSTEM";
        String fullName = "System Process";

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equalsIgnoreCase("anonymousUser")) {
            username = auth.getName();
            role = auth.getAuthorities().toString();
            fullName = auth.getName(); // Fallback if no full name helper
        }

        String ipAddress = "0.0.0.0";
        String browser = "Unknown";
        String os = "Unknown";
        String requestUrl = "";
        String httpMethod = "";

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            ipAddress = getClientIp(request);
            requestUrl = request.getRequestURI();
            httpMethod = request.getMethod();

            String userAgent = request.getHeader("User-Agent");
            if (userAgent != null) {
                // Basic parsing
                if (userAgent.contains("MSIE") || userAgent.contains("Trident")) {
                    browser = "Internet Explorer";
                } else if (userAgent.contains("Edge")) {
                    browser = "Edge";
                } else if (userAgent.contains("Chrome")) {
                    browser = "Chrome";
                } else if (userAgent.contains("Safari")) {
                    browser = "Safari";
                } else if (userAgent.contains("Firefox")) {
                    browser = "Firefox";
                }

                if (userAgent.contains("Windows")) {
                    os = "Windows";
                } else if (userAgent.contains("Mac")) {
                    os = "Mac OS";
                } else if (userAgent.contains("Linux")) {
                    os = "Linux";
                } else if (userAgent.contains("Android")) {
                    os = "Android";
                } else if (userAgent.contains("iPhone")) {
                    os = "iOS";
                }
            }
        }

        AuditLog log = AuditLog.builder()
                .username(username)
                .fullName(fullName)
                .role(role)
                .module(module)
                .action(action)
                .description(description)
                .entityName(entityName)
                .entityId(entityId)
                .ipAddress(ipAddress)
                .browser(browser)
                .operatingSystem(os)
                .requestUrl(requestUrl)
                .httpMethod(httpMethod)
                .success(success)
                .errorMessage(errorMessage)
                .build();

        auditLogRepository.save(log);
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isEmpty()) {
            return xf.split(",")[0];
        }
        return request.getRemoteAddr();
    }

    // ── Export Excel ──────────────────────────────────────────────────────────
    private byte[] exportToExcel(List<AuditLog> logs) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Audit Logs");

        org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        String[] headers = {"Timestamp", "User", "Role", "Module", "Action", "Description", "IP Address", "Status", "Error Message"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        int rowIdx = 1;
        for (AuditLog l : logs) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(l.getTimestamp() != null ? l.getTimestamp().format(formatter) : "");
            row.createCell(1).setCellValue(l.getUsername() != null ? l.getUsername() : "");
            row.createCell(2).setCellValue(l.getRole() != null ? l.getRole() : "");
            row.createCell(3).setCellValue(l.getModule() != null ? l.getModule().name() : "");
            row.createCell(4).setCellValue(l.getAction() != null ? l.getAction().name() : "");
            row.createCell(5).setCellValue(l.getDescription() != null ? l.getDescription() : "");
            row.createCell(6).setCellValue(l.getIpAddress() != null ? l.getIpAddress() : "");
            row.createCell(7).setCellValue(l.getSuccess() ? "SUCCESS" : "FAILED");
            row.createCell(8).setCellValue(l.getErrorMessage() != null ? l.getErrorMessage() : "");
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();
        return bos.toByteArray();
    }

    // ── Export PDF ────────────────────────────────────────────────────────────
    private byte[] exportToPdf(List<AuditLog> logs) {
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 54, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph header = new Paragraph("SMART OLD AGE HOME - AUDIT LOGS", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BaseColor.DARK_GRAY));
            header.setAlignment(Element.ALIGN_CENTER);
            header.setSpacingAfter(20);
            document.add(header);

            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.8f, 1.2f, 1.2f, 1.2f, 2.5f, 1.2f, 1f, 1.5f});

            String[] headers = {"Timestamp", "User", "Module", "Action", "Description", "IP Address", "Status", "Error"};
            com.itextpdf.text.Font headerCellFont =
        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BaseColor.WHITE);
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerCellFont));
                cell.setBackgroundColor(new BaseColor(37, 99, 235));
                cell.setPadding(5);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
            com.itextpdf.text.Font rowFont =
        FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.BLACK);
            for (AuditLog l : logs) {
                table.addCell(new PdfPCell(new Phrase(l.getTimestamp() != null ? l.getTimestamp().format(formatter) : "", rowFont)));
                table.addCell(new PdfPCell(new Phrase(l.getUsername(), rowFont)));
                table.addCell(new PdfPCell(new Phrase(l.getModule() != null ? l.getModule().name() : "", rowFont)));
                table.addCell(new PdfPCell(new Phrase(l.getAction() != null ? l.getAction().name() : "", rowFont)));
                table.addCell(new PdfPCell(new Phrase(l.getDescription(), rowFont)));
                table.addCell(new PdfPCell(new Phrase(l.getIpAddress(), rowFont)));
                table.addCell(new PdfPCell(new Phrase(l.getSuccess() ? "Success" : "Failed", rowFont)));
                table.addCell(new PdfPCell(new Phrase(l.getErrorMessage() != null ? l.getErrorMessage() : "", rowFont)));
            }

            document.add(table);
            document.close();
        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    // ── Export CSV ────────────────────────────────────────────────────────────
    private byte[] exportToCsv(List<AuditLog> logs) {
        StringBuilder sb = new StringBuilder();
        sb.append("Timestamp,User,Role,Module,Action,Description,IP Address,Status,Error\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        for (AuditLog l : logs) {
            sb.append("\"").append(l.getTimestamp() != null ? l.getTimestamp().format(formatter) : "").append("\",");
            sb.append("\"").append(l.getUsername() != null ? l.getUsername().replace("\"", "\"\"") : "").append("\",");
            sb.append("\"").append(l.getRole() != null ? l.getRole().replace("\"", "\"\"") : "").append("\",");
            sb.append("\"").append(l.getModule() != null ? l.getModule().name() : "").append("\",");
            sb.append("\"").append(l.getAction() != null ? l.getAction().name() : "").append("\",");
            sb.append("\"").append(l.getDescription() != null ? l.getDescription().replace("\"", "\"\"") : "").append("\",");
            sb.append("\"").append(l.getIpAddress() != null ? l.getIpAddress() : "").append("\",");
            sb.append("\"").append(l.getSuccess() ? "SUCCESS" : "FAILED").append("\",");
            sb.append("\"").append(l.getErrorMessage() != null ? l.getErrorMessage().replace("\"", "\"\"") : "").append("\"\n");
        }
        return sb.toString().getBytes();
    }
}
