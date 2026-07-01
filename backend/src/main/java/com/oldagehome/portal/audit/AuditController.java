package com.oldagehome.portal.audit;

import com.oldagehome.portal.common.AppConstants;
import com.oldagehome.portal.common.PaginationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/admin/audit")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditService auditService;

    @Autowired
    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public String listLogs(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = AppConstants.Pagination.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(value = "size", defaultValue = AppConstants.Pagination.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(value = "sort", defaultValue = AppConstants.Pagination.DEFAULT_SORT_BY) String sort,
            @RequestParam(value = "direction", defaultValue = AppConstants.Pagination.DEFAULT_SORT_DIRECTION) String direction,
            Model model) {

        Pageable pageable = buildAuditPageable(page, size, sort, direction);
        Page<AuditLog> auditPage = auditService.getAuditLogs(keyword, pageable);

        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);
        LinkedHashMap<String, Object> paginationParams = new LinkedHashMap<>();
        paginationParams.put("keyword", keyword);
        paginationParams.put("sort", sort);
        paginationParams.put("direction", direction);
        model.addAttribute("paginationQuery", PaginationUtils.buildQueryString(paginationParams));

        model.addAttribute("auditPage", auditPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("activePage", "audit");

        // Dashboard Stats
        model.addAttribute("todayActivities", auditService.countTodayActivities());
        model.addAttribute("successfulActivities", auditService.countSuccessfulActivities());
        model.addAttribute("failedActivities", auditService.countFailedActivities());
        model.addAttribute("totalRecords", auditService.getTotalRecords());
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        var latestLogin = auditService.getLatestLogin();
        var latestUpdate = auditService.getLatestSystemUpdate();
        model.addAttribute("latestLogin", latestLogin != null ? latestLogin.format(formatter) : "Never");
        model.addAttribute("latestUpdate", latestUpdate != null ? latestUpdate.format(formatter) : "Never");

        model.addAttribute("modules", AuditModule.values());
        model.addAttribute("actions", AuditAction.values());

        return "audit/list";
    }

    @GetMapping("/filter")
    public String filterLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "page", defaultValue = AppConstants.Pagination.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(value = "size", defaultValue = AppConstants.Pagination.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(value = "sort", defaultValue = AppConstants.Pagination.DEFAULT_SORT_BY) String sort,
            @RequestParam(value = "direction", defaultValue = AppConstants.Pagination.DEFAULT_SORT_DIRECTION) String direction,
            Model model) {

        Pageable pageable = buildAuditPageable(page, size, sort, direction);
        Page<AuditLog> auditPage = auditService.filterLogs(username, module, action, success, startDate, endDate, pageable);

        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);
        LinkedHashMap<String, Object> paginationParams = new LinkedHashMap<>();
        paginationParams.put("username", username);
        paginationParams.put("module", module);
        paginationParams.put("action", action);
        paginationParams.put("success", success);
        paginationParams.put("startDate", startDate);
        paginationParams.put("endDate", endDate);
        paginationParams.put("sort", sort);
        paginationParams.put("direction", direction);
        model.addAttribute("paginationQuery", PaginationUtils.buildQueryString(paginationParams));

        model.addAttribute("auditPage", auditPage);
        model.addAttribute("username", username);
        model.addAttribute("module", module);
        model.addAttribute("action", action);
        model.addAttribute("success", success);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("activePage", "audit");

        model.addAttribute("todayActivities", auditService.countTodayActivities());
        model.addAttribute("successfulActivities", auditService.countSuccessfulActivities());
        model.addAttribute("failedActivities", auditService.countFailedActivities());
        model.addAttribute("totalRecords", auditService.getTotalRecords());
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        var latestLogin = auditService.getLatestLogin();
        var latestUpdate = auditService.getLatestSystemUpdate();
        model.addAttribute("latestLogin", latestLogin != null ? latestLogin.format(formatter) : "Never");
        model.addAttribute("latestUpdate", latestUpdate != null ? latestUpdate.format(formatter) : "Never");

        model.addAttribute("modules", AuditModule.values());
        model.addAttribute("actions", AuditAction.values());

        return "audit/list";
    }

    @GetMapping("/view/{id}")
    public String viewLog(@PathVariable("id") Long id, Model model) {
        AuditLog log = auditService.getAuditLogById(id);

        model.addAttribute("log", log);
        model.addAttribute("activePage", "audit");
        return "audit/view";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "EXCEL") String format) throws IOException {

        Page<AuditLog> auditPage = auditService.filterLogs(username, module, action, success, startDate, endDate, Pageable.unpaged());
        List<AuditLog> logs = auditPage.getContent();
        byte[] data = auditService.exportLogs(logs, format);

        String filename = "audit_report_" + System.currentTimeMillis();
        MediaType mediaType;
        if ("PDF".equalsIgnoreCase(format)) {
            filename += ".pdf";
            mediaType = MediaType.APPLICATION_PDF;
        } else if ("CSV".equalsIgnoreCase(format)) {
            filename += ".csv";
            mediaType = MediaType.parseMediaType("text/csv");
        } else {
            filename += ".xlsx";
            mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(mediaType)
                .body(data);
    }

    private Pageable buildAuditPageable(int page, int size, String sort, String direction) {
        String normalizedSort = sort != null ? sort.trim().toLowerCase() : "";
        String property;

        switch (normalizedSort) {
            case "user":
            case "username":
                property = "username";
                break;
            case "module":
                property = "module";
                break;
            case "action":
                property = "action";
                break;
            case "dateadded":
            case "timestamp":
                property = "timestamp";
                break;
            default:
                property = AppConstants.Pagination.DEFAULT_SORT_BY;
                break;
        }

        return PaginationUtils.buildPageable(page, size, property, direction, AppConstants.Pagination.DEFAULT_SORT_BY);
    }
}
