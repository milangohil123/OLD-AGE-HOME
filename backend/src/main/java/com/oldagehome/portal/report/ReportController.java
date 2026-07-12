package com.oldagehome.portal.report;

import com.oldagehome.portal.common.AppConstants;
import com.oldagehome.portal.common.PaginationUtils;
import com.oldagehome.portal.resident.Resident;
import com.oldagehome.portal.resident.ResidentService;
import com.oldagehome.portal.resident.ResidentStatus;
import com.oldagehome.portal.donor.Donor;
import com.oldagehome.portal.donor.DonorService;
import com.oldagehome.portal.donor.DonorStatus;
import com.oldagehome.portal.donor.DonationType;
import com.oldagehome.portal.inventory.Inventory;
import com.oldagehome.portal.inventory.InventoryService;
import com.oldagehome.portal.inventory.InventoryStatus;
import com.oldagehome.portal.inventory.MedicineCategory;

import com.oldagehome.portal.pdf.ResidentReportGenerator;
import com.oldagehome.portal.pdf.DonorReportGenerator;
import com.oldagehome.portal.pdf.DonationReportGenerator;
import com.oldagehome.portal.pdf.InventoryReportGenerator;

import com.oldagehome.portal.excel.ResidentReportExporter;
import com.oldagehome.portal.excel.DonorReportExporter;
import com.oldagehome.portal.excel.DonationReportExporter;
import com.oldagehome.portal.excel.InventoryReportExporter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;
    private final ResidentService residentService;
    private final DonorService donorService;
    private final InventoryService inventoryService;

    @Autowired
    public ReportController(ReportService reportService,
                            ResidentService residentService,
                            DonorService donorService,
                            InventoryService inventoryService) {
        this.reportService = reportService;
        this.residentService = residentService;
        this.donorService = donorService;
        this.inventoryService = inventoryService;
    }

    // GET /reports - Dashboard
    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("activePage", "reports");
        
        // Dashboard Stats
        List<Resident> residents = residentService.getAllResidents();
        model.addAttribute("totalResidents", residents.size());
        model.addAttribute("totalDonors", donorService.countTotalDonors());
        model.addAttribute("totalDonations", donorService.countTodayDonations() + donorService.countThisMonthDonations()); // approximate or total donation entries count
        model.addAttribute("totalDonationAmount", donorService.sumTotalDonationAmount());
        model.addAttribute("totalMedicines", inventoryService.countTotalMedicines());
        model.addAttribute("lowStock", inventoryService.countLowStock());
        model.addAttribute("expiredMedicines", inventoryService.countExpired());
        model.addAttribute("availableMedicines", inventoryService.countAvailable());

        // Recents
        model.addAttribute("recentDonations", donorService.getAllDonors()); // list has latest donors at the end or sorting can be done
        model.addAttribute("recentResidents", residents);

        return "reports/dashboard";
    }

    // GET /reports/residents - Resident Filters
    @GetMapping("/residents")
    public String residentsPage(Model model) {
        model.addAttribute("activePage", "reports");
        model.addAttribute("statuses", ResidentStatus.values());
        return "reports/residents";
    }

    // GET /reports/donors - Donor Filters
    @GetMapping("/donors")
    public String donorsPage(Model model) {
        model.addAttribute("activePage", "reports");
        model.addAttribute("statuses", DonorStatus.values());
        model.addAttribute("donationTypes", DonationType.values());
        return "reports/donors";
    }

    // GET /reports/donations - Donation Filters
    @GetMapping("/donations")
    public String donationsPage(Model model) {
        model.addAttribute("activePage", "reports");
        model.addAttribute("donationTypes", DonationType.values());
        return "reports/donations";
    }

    // GET /reports/inventory - Inventory Filters
    @GetMapping("/inventory")
    public String inventoryPage(Model model) {
        model.addAttribute("activePage", "reports");
        model.addAttribute("statuses", InventoryStatus.values());
        model.addAttribute("categories", MedicineCategory.values());
        return "reports/inventory";
    }

    // GET /reports/residents/pdf
    @GetMapping("/residents/pdf")
    public ResponseEntity<byte[]> residentsPdf(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Principal principal) {
        
        List<Resident> data = reportService.getResidentsReport(status, keyword, month, year, startDate, endDate, Pageable.unpaged()).getContent();
        String user = principal != null ? principal.getName() : "Administrator";
        byte[] pdf = ResidentReportGenerator.generatePdf(data, "Resident Management Report", user);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=residents_report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // GET /reports/residents/excel
    @GetMapping("/residents/excel")
    public ResponseEntity<byte[]> residentsExcel(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws IOException {
        
        List<Resident> data = reportService.getResidentsReport(status, keyword, month, year, startDate, endDate, Pageable.unpaged()).getContent();
        byte[] excel = ResidentReportExporter.exportExcel(data, "Resident Management Report");
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=residents_report.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }

    // GET /reports/donors/pdf
    @GetMapping("/donors/pdf")
    public ResponseEntity<byte[]> donorsPdf(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String donationType,
            Principal principal) {
        
        List<Donor> data = reportService.getDonorsReport(status, keyword, donationType, Pageable.unpaged()).getContent();
        String user = principal != null ? principal.getName() : "Administrator";
        byte[] pdf = DonorReportGenerator.generatePdf(data, "Donor List Report", user);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=donors_report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // GET /reports/donors/excel
    @GetMapping("/donors/excel")
    public ResponseEntity<byte[]> donorsExcel(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String donationType) throws IOException {
        
        List<Donor> data = reportService.getDonorsReport(status, keyword, donationType, Pageable.unpaged()).getContent();
        byte[] excel = DonorReportExporter.exportExcel(data, "Donor List Report");
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=donors_report.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }

    // GET /reports/donations/pdf
    @GetMapping("/donations/pdf")
    public ResponseEntity<byte[]> donationsPdf(
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String donationType,
            Principal principal) {
        
        List<Donor> data = reportService.getDonationsReport(paymentMethod, month, year, startDate, endDate, donationType, Pageable.unpaged()).getContent();
        String user = principal != null ? principal.getName() : "Administrator";
        byte[] pdf = DonationReportGenerator.generatePdf(data, "Donation Collection Report", user);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=donations_report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // GET /reports/donations/excel
    @GetMapping("/donations/excel")
    public ResponseEntity<byte[]> donationsExcel(
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String donationType) throws IOException {
        
        List<Donor> data = reportService.getDonationsReport(paymentMethod, month, year, startDate, endDate, donationType, Pageable.unpaged()).getContent();
        byte[] excel = DonationReportExporter.exportExcel(data, "Donation Collection Report");
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=donations_report.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }

    // GET /reports/inventory/pdf
    @GetMapping("/inventory/pdf")
    public ResponseEntity<byte[]> inventoryPdf(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Principal principal) {
        
        List<Inventory> data = reportService.getInventoryReport(status, keyword, category, startDate, endDate, Pageable.unpaged()).getContent();
        String user = principal != null ? principal.getName() : "Administrator";
        byte[] pdf = InventoryReportGenerator.generatePdf(data, "Medicine Inventory Report", user);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventory_report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // GET /reports/inventory/excel
    @GetMapping("/inventory/excel")
    public ResponseEntity<byte[]> inventoryExcel(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws IOException {
        
        List<Inventory> data = reportService.getInventoryReport(status, keyword, category, startDate, endDate, Pageable.unpaged()).getContent();
        byte[] excel = InventoryReportExporter.exportExcel(data, "Medicine Inventory Report");
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventory_report.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }

    // GET /reports/preview - Preview Report Data
    @GetMapping("/preview")
    public String previewReport(
            @RequestParam String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String donationType,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String category,
            @RequestParam(value = "page", defaultValue = AppConstants.Pagination.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(value = "size", defaultValue = AppConstants.Pagination.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(value = "sort", defaultValue = AppConstants.Pagination.DEFAULT_SORT_BY) String sort,
            @RequestParam(value = "direction", defaultValue = AppConstants.Pagination.DEFAULT_SORT_DIRECTION) String direction,
            Model model) {
        
        model.addAttribute("activePage", "reports");
        model.addAttribute("type", type);
        model.addAttribute("pageSize", size);
        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);
        
        // Pass filter values back to keep download buttons synchronized
        model.addAttribute("status", status);
        model.addAttribute("keyword", keyword);
        model.addAttribute("month", month);
        model.addAttribute("year", year);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("donationType", donationType);
        model.addAttribute("paymentMethod", paymentMethod);
        model.addAttribute("category", category);

        Pageable pageable = buildReportPageable(type, page, size, sort, direction);

        Map<String, Object> paginationParams = new LinkedHashMap<>();
        paginationParams.put("type", type);
        paginationParams.put("status", status);
        paginationParams.put("keyword", keyword);
        paginationParams.put("month", month);
        paginationParams.put("year", year);
        paginationParams.put("startDate", startDate);
        paginationParams.put("endDate", endDate);
        paginationParams.put("donationType", donationType);
        paginationParams.put("paymentMethod", paymentMethod);
        paginationParams.put("category", category);
        paginationParams.put("sort", sort);
        paginationParams.put("direction", direction);
        model.addAttribute("paginationQuery", PaginationUtils.buildQueryString(paginationParams));

        if (type.equalsIgnoreCase("residents")) {
            Page<Resident> data = reportService.getResidentsReport(status, keyword, month, year, startDate, endDate, pageable);
            model.addAttribute("reportPage", data);
            model.addAttribute("title", "Resident Management Preview Report");
        } else if (type.equalsIgnoreCase("donors")) {
            Page<Donor> data = reportService.getDonorsReport(status, keyword, donationType, pageable);
            model.addAttribute("reportPage", data);
            model.addAttribute("title", "Donor List Preview Report");
        } else if (type.equalsIgnoreCase("donations")) {
            Page<Donor> data = reportService.getDonationsReport(paymentMethod, month, year, startDate, endDate, donationType, pageable);
            model.addAttribute("reportPage", data);
            model.addAttribute("title", "Donation Collection Preview Report");
        } else if (type.equalsIgnoreCase("inventory")) {
            Page<Inventory> data = reportService.getInventoryReport(status, keyword, category, startDate, endDate, pageable);
            model.addAttribute("reportPage", data);
            model.addAttribute("title", "Medicine Inventory Preview Report");
        }

        return "reports/preview";
    }

    private Pageable buildReportPageable(String type, int page, int size, String sort, String direction) {
        String normalizedSort = sort != null ? sort.trim().toLowerCase() : "";
        String property = AppConstants.Pagination.DEFAULT_SORT_BY;
        String resolvedDirection = direction;

        if (type != null && type.equalsIgnoreCase("residents")) {
            switch (normalizedSort) {
                case "name":
                case "fullname":
                    property = "fullName";
                    break;

                case "age":
                    property = "dateOfBirth";
                    resolvedDirection = "desc".equalsIgnoreCase(direction) ? "asc" : "desc";
                    break;
                case "dateadded":
                case "joiningdate":
                    property = "joiningDate";
                    break;
                default:
                    property = AppConstants.Pagination.DEFAULT_SORT_BY;
                    break;
            }
        } else if (type != null && type.equalsIgnoreCase("donors")) {
            switch (normalizedSort) {
                case "name":
                case "fullname":
                    property = "fullName";
                    break;
                case "dateadded":
                case "donationdate":
                    property = "donationDate";
                    break;
                case "amount":
                case "donationamount":
                    property = "donationAmount";
                    break;
                default:
                    property = AppConstants.Pagination.DEFAULT_SORT_BY;
                    break;
            }
        } else if (type != null && type.equalsIgnoreCase("donations")) {
            switch (normalizedSort) {
                case "name":
                case "fullname":
                    property = "fullName";
                    break;
                case "dateadded":
                case "donationdate":
                    property = "donationDate";
                    break;
                case "amount":
                case "donationamount":
                    property = "donationAmount";
                    break;
                default:
                    property = AppConstants.Pagination.DEFAULT_SORT_BY;
                    break;
            }
        } else if (type != null && type.equalsIgnoreCase("inventory")) {
            switch (normalizedSort) {
                case "name":
                case "medicinename":
                    property = "medicineName";
                    break;
                case "code":
                case "medicinecode":
                    property = "medicineCode";
                    break;
                case "category":
                    property = "category";
                    break;
                case "dateadded":
                case "purchasedate":
                    property = "purchaseDate";
                    break;
                default:
                    property = AppConstants.Pagination.DEFAULT_SORT_BY;
                    break;
            }
        }

        return PaginationUtils.buildPageable(page, size, property, resolvedDirection, AppConstants.Pagination.DEFAULT_SORT_BY);
    }
}
