package com.oldagehome.portal.foodschedule;

import com.oldagehome.portal.audit.AuditAction;
import com.oldagehome.portal.audit.AuditModule;
import com.oldagehome.portal.audit.AuditService;
import com.oldagehome.portal.common.AppConstants;
import com.oldagehome.portal.common.PaginationUtils;
import com.oldagehome.portal.donor.Donor;
import com.oldagehome.portal.donor.DonorRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Handles all Food Schedule routes.
 *
 * GET  /food-schedule              — Main list page
 * POST /food-schedule/save         — Save new record
 * POST /food-schedule/update/{id}  — Update existing record
 * GET  /food-schedule/delete/{id}  — Delete record
 * GET  /food-schedule/rate         — JSON: fetch donation rate
 * GET  /food-schedule/search       — Paginated + filtered history search
 * GET  /food-schedule/api/item/{id}— JSON: get single item for edit modal
 * GET  /food-schedule/api/donors   — JSON: list food donors
 *
 * Print endpoints (new — open in new tab, print-ready Thymeleaf templates):
 * GET  /food-schedule/print/today    — Today's full schedule
 * GET  /food-schedule/print/kitchen  — Kitchen copy (menu items only)
 * GET  /food-schedule/print/sponsor  — Sponsor acknowledgement copy
 * GET  /food-schedule/print/donation — Donation summary copy
 */
@Controller
@RequestMapping("/food-schedule")
public class FoodScheduleController {

    private final FoodScheduleService foodScheduleService;
    private final DonorRepository donorRepository;
    private final AuditService auditService;

    @Autowired
    public FoodScheduleController(FoodScheduleService foodScheduleService,
            DonorRepository donorRepository,
            AuditService auditService) {
        this.foodScheduleService = foodScheduleService;
        this.donorRepository = donorRepository;
        this.auditService = auditService;
    }

    // ── Shared model population ────────────────────────────────────────────────

    /**
     * Populates all model attributes needed by the list page.
     * Uses getEnrichedStats() so KPIs and analytics come from the backend.
     */
    private void populateListPageModel(Model model, FoodScheduleDTO formDto) {
        model.addAttribute("todaySchedule", foodScheduleService.findTodaysSchedule());
        model.addAttribute("stats", foodScheduleService.getEnrichedStats());

        List<Donor> foodDonors = donorRepository.findAll().stream()
                .filter(d -> "Food Donation".equals(d.getDonationCategory()))
                .sorted(Comparator.comparing(Donor::getFullName))
                .toList();

        model.addAttribute("foodDonors", foodDonors);
        model.addAttribute("formDto", formDto);
        model.addAttribute("mealTypes", MealType.values());
        model.addAttribute("sponsorshipTypes", SponsorshipType.values());
        model.addAttribute("today", LocalDate.now());

        // Always initialise filter state so the Thymeleaf template never hits missing-variable errors
        model.addAttribute("searchPerformed", false);
        model.addAttribute("srFromDate", null);
        model.addAttribute("srToDate", null);
        model.addAttribute("srMealType", null);
        model.addAttribute("srSponsorshipType", null);
        model.addAttribute("srDonorKeyword", null);
        model.addAttribute("srMinAmount", null);
        model.addAttribute("srMaxAmount", null);
        model.addAttribute("paginationQuery", "");
    }

    /**
     * Fetches the first page of the history table (newest first).
     * Used on initial page load.
     */
    private void populateDefaultHistoryPage(Model model) {
        Pageable pageable = PaginationUtils.buildPageable(0, 10, "scheduleDate", "desc", "scheduleDate");
        Page<FoodSchedule> historyPage = foodScheduleService.searchSchedulePaged(
                null, null, null, null, null, null, null, pageable);
        addHistoryPageAttributes(model, historyPage, 0, 10, "scheduleDate", "desc");
    }

    /**
     * Adds paginated history table attributes to the model.
     */
    private void addHistoryPageAttributes(Model model, Page<FoodSchedule> historyPage,
                                           int page, int size, String sort, String direction) {
        model.addAttribute("historyPage", historyPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", historyPage.getTotalPages());
        model.addAttribute("pageSize", size);
        model.addAttribute("totalElements", historyPage.getTotalElements());
        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);
    }

    // ── Main page ─────────────────────────────────────────────────────────────

    @GetMapping
    public String listPage(Model model, Authentication authentication) {
        FoodScheduleDTO formDto = new FoodScheduleDTO();
        formDto.setScheduleDate(LocalDate.now());

        populateListPageModel(model, formDto);
        populateDefaultHistoryPage(model);

        return "food-schedule/list";
    }

    // ── Save (new record) ─────────────────────────────────────────────────────

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("formDto") FoodScheduleDTO dto,
            BindingResult result,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Additional validation: either donorId or manualDonorName must be present
        if (!dto.isDonorNotFound() && dto.getDonorId() == null) {
            result.rejectValue("donorId", "required", "Please select a Food Donor.");
        }
        if (dto.isDonorNotFound() && (dto.getManualDonorName() == null || dto.getManualDonorName().isBlank())) {
            result.rejectValue("manualDonorName", "required", "Manual Donor Name is required.");
        }

        if (result.hasErrors()) {
            populateListPageModel(model, dto);
            populateDefaultHistoryPage(model);
            model.addAttribute("openAddModalOnError", true);
            model.addAttribute("searchPerformed", false);
            model.addAttribute("errorMessage", "Please fix the validation errors and try again.");
            return "food-schedule/list";
        }

        try {
            String username = authentication.getName();
            FoodSchedule saved = foodScheduleService.save(dto, username);

            auditService.logActivity(AuditModule.FOOD_SCHEDULE, AuditAction.CREATE,
                    "Food schedule created for " + dto.getScheduleDate() + " - " + dto.getMealType().getDisplayName(),
                    "FoodSchedule", saved.getId(), true, null);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Food schedule for " + dto.getMealType().getDisplayName() + " saved successfully.");
        } catch (Exception e) {
            auditService.logActivity(AuditModule.FOOD_SCHEDULE, AuditAction.CREATE,
                    "Failed to save food schedule: " + e.getMessage(),
                    "FoodSchedule", null, false, e.getMessage());
            populateListPageModel(model, dto);
            populateDefaultHistoryPage(model);
            model.addAttribute("openAddModalOnError", true);
            model.addAttribute("searchPerformed", false);
            model.addAttribute("errorMessage", "Failed to save: " + e.getMessage());
            return "food-schedule/list";
        }

        return "redirect:/food-schedule";
    }

    // ── Update (existing record) ──────────────────────────────────────────────

    @PostMapping("/update/{id}")
    public String update(@PathVariable Long id,
            @Valid @ModelAttribute("formDto") FoodScheduleDTO dto,
            BindingResult result,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (!dto.isDonorNotFound() && dto.getDonorId() == null) {
            result.rejectValue("donorId", "required", "Please select a Food Donor.");
        }
        if (dto.isDonorNotFound() && (dto.getManualDonorName() == null || dto.getManualDonorName().isBlank())) {
            result.rejectValue("manualDonorName", "required", "Manual Donor Name is required.");
        }

        if (result.hasErrors()) {
            populateListPageModel(model, dto);
            populateDefaultHistoryPage(model);
            model.addAttribute("openEditModalOnError", true);
            model.addAttribute("editRecordId", id);
            model.addAttribute("searchPerformed", false);
            model.addAttribute("errorMessage", "Please fix the validation errors and try again.");
            return "food-schedule/list";
        }

        try {
            String username = authentication.getName();
            foodScheduleService.update(id, dto, username);

            auditService.logActivity(AuditModule.FOOD_SCHEDULE, AuditAction.UPDATE,
                    "Food schedule updated: ID " + id,
                    "FoodSchedule", id, true, null);

            redirectAttributes.addFlashAttribute("successMessage", "Food schedule updated successfully.");
        } catch (Exception e) {

            e.printStackTrace();

            auditService.logActivity(
                    AuditModule.FOOD_SCHEDULE,
                    AuditAction.CREATE,
                    "Failed to save food schedule: " + e.getMessage(),
                    "FoodSchedule",
                    null,
                    false,
                    e.getMessage());

            throw e;
        }
        return "redirect:/food-schedule";
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            foodScheduleService.delete(id, username);

            auditService.logActivity(AuditModule.FOOD_SCHEDULE, AuditAction.DELETE,
                    "Food schedule deleted: ID " + id,
                    "FoodSchedule", id, true, null);

            redirectAttributes.addFlashAttribute("successMessage", "Food schedule deleted successfully.");
        } catch (Exception e) {
            auditService.logActivity(AuditModule.FOOD_SCHEDULE, AuditAction.DELETE,
                    "Failed to delete food schedule: " + e.getMessage(),
                    "FoodSchedule", id, false, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete: " + e.getMessage());
        }

        return "redirect:/food-schedule";
    }

    // ── Search history (paginated, extended filters) ───────────────────────────

    @GetMapping("/search")
    public String search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String mealType,
            @RequestParam(required = false) String sponsorshipType,
            @RequestParam(required = false) String donorKeyword,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(value = "page", defaultValue = AppConstants.Pagination.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(value = "size", defaultValue = AppConstants.Pagination.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(value = "sort", defaultValue = "scheduleDate") String sort,
            @RequestParam(value = "direction", defaultValue = AppConstants.Pagination.DEFAULT_SORT_DIRECTION) String direction,
            Model model,
            Authentication authentication) {

        MealType mealTypeEnum = null;
        if (mealType != null && !mealType.isBlank()) {
            try {
                mealTypeEnum = MealType.valueOf(mealType);
            } catch (IllegalArgumentException ignored) {
            }
        }

        SponsorshipType sponsorshipTypeEnum = null;
        if (sponsorshipType != null && !sponsorshipType.isBlank()) {
            try {
                sponsorshipTypeEnum = SponsorshipType.valueOf(sponsorshipType);
            } catch (IllegalArgumentException ignored) {
            }
        }

        // Build pageable using PaginationUtils (mirrors ResidentController pattern)
        Pageable pageable = PaginationUtils.buildPageable(page, size, sort, direction, "scheduleDate");

        Page<FoodSchedule> historyPage = foodScheduleService.searchSchedulePaged(
                fromDate, toDate, mealTypeEnum, sponsorshipTypeEnum, donorKeyword,
                minAmount, maxAmount, pageable);

        // Reload common page data
        populateListPageModel(model, buildDefaultFormDto());

        // History table pagination attributes
        addHistoryPageAttributes(model, historyPage, page, size, sort, direction);

        // Build query string for pagination links (preserves filter params across pages)
        LinkedHashMap<String, Object> paginationParams = new LinkedHashMap<>();
        paginationParams.put("fromDate", fromDate);
        paginationParams.put("toDate", toDate);
        paginationParams.put("mealType", mealType);
        paginationParams.put("sponsorshipType", sponsorshipType);
        paginationParams.put("donorKeyword", donorKeyword);
        paginationParams.put("minAmount", minAmount);
        paginationParams.put("maxAmount", maxAmount);
        paginationParams.put("sort", sort);
        paginationParams.put("direction", direction);
        model.addAttribute("paginationQuery", PaginationUtils.buildQueryString(paginationParams));

        model.addAttribute("searchPerformed", true);

        // Retain filter values in the view
        model.addAttribute("srFromDate", fromDate);
        model.addAttribute("srToDate", toDate);
        model.addAttribute("srMealType", mealType);
        model.addAttribute("srSponsorshipType", sponsorshipType);
        model.addAttribute("srDonorKeyword", donorKeyword);
        model.addAttribute("srMinAmount", minAmount);
        model.addAttribute("srMaxAmount", maxAmount);

        return "food-schedule/list";
    }

    // ── REST API: fetch donation rate ─────────────────────────────────────────

    @GetMapping("/rate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRate(
            @RequestParam String mealType,
            @RequestParam String sponsorshipType) {

        Optional<BigDecimal> amount = foodScheduleService.getRateAmount(mealType, sponsorshipType);

        Map<String, Object> response = new HashMap<>();
        if (amount.isPresent()) {
            response.put("found", true);
            response.put("amount", amount.get());
        } else {
            response.put("found", false);
            response.put("message", "No rate configured for this combination.");
        }

        return ResponseEntity.ok(response);
    }

    // ── REST API: get single item for edit modal ───────────────────────────────

    @GetMapping("/api/item/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getItem(@PathVariable Long id) {
        return foodScheduleService.findById(id)
                .map(fs -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", fs.getId());
                    data.put("scheduleDate", fs.getScheduleDate().toString());
                    data.put("mealType", fs.getMealType().name());
                    data.put("servingTime", fs.getServingTime().toString());
                    data.put("menuItems", fs.getMenuItems());
                    data.put("donorId", fs.getDonor() != null ? fs.getDonor().getId() : null);
                    data.put("manualDonorName", fs.getManualDonorName());
                    data.put("donorNotFound", fs.getDonor() == null && fs.getManualDonorName() != null);
                    data.put("sponsorshipType", fs.getSponsorshipType().name());
                    data.put("amount", fs.getAmount());
                    data.put("notes", fs.getNotes());
                    return ResponseEntity.ok(data);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Print Endpoints ───────────────────────────────────────────────────────

    /**
     * Today's full schedule — for administrators.
     * Opens food-schedule/print/today-schedule.html in a new tab.
     */
    @GetMapping("/print/today")
    public String printToday(Model model) {
        model.addAttribute("todaySchedule", foodScheduleService.findTodaysSchedule());
        model.addAttribute("stats", foodScheduleService.getEnrichedStats());
        model.addAttribute("today", LocalDate.now());
        return "food-schedule/print/today-schedule";
    }

    /**
     * Kitchen copy — simplified menu-only view for kitchen staff.
     * Hides all financial and donor data.
     */
    @GetMapping("/print/kitchen")
    public String printKitchen(Model model) {
        model.addAttribute("todaySchedule", foodScheduleService.findTodaysSchedule());
        model.addAttribute("today", LocalDate.now());
        return "food-schedule/print/kitchen-copy";
    }

    /**
     * Sponsor copy — donor acknowledgement format.
     * Shows donor names, sponsorship types, amounts.
     */
    @GetMapping("/print/sponsor")
    public String printSponsor(Model model) {
        model.addAttribute("todaySchedule", foodScheduleService.findTodaysSchedule());
        model.addAttribute("stats", foodScheduleService.getEnrichedStats());
        model.addAttribute("today", LocalDate.now());
        return "food-schedule/print/sponsor-copy";
    }

    /**
     * Donation copy — receipt-style donation summary.
     * Styled consistently with the existing money receipt template.
     */
    @GetMapping("/print/donation")
    public String printDonation(Model model) {
        model.addAttribute("todaySchedule", foodScheduleService.findTodaysSchedule());
        model.addAttribute("stats", foodScheduleService.getEnrichedStats());
        model.addAttribute("today", LocalDate.now());
        return "food-schedule/print/donation-copy";
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private FoodScheduleDTO buildDefaultFormDto() {
        FoodScheduleDTO dto = new FoodScheduleDTO();
        dto.setScheduleDate(LocalDate.now());
        return dto;
    }
}
