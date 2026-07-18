package com.oldagehome.portal.foodschedule;

import com.oldagehome.portal.audit.AuditAction;
import com.oldagehome.portal.audit.AuditModule;
import com.oldagehome.portal.audit.AuditService;
import com.oldagehome.portal.donor.Donor;
import com.oldagehome.portal.donor.DonorRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.time.LocalTime;
import java.util.*;

/**
 * Handles all Food Schedule routes.
 *
 * GET  /food-schedule             — Main list page
 * POST /food-schedule/save        — Save new record
 * POST /food-schedule/update/{id} — Update existing record
 * GET  /food-schedule/delete/{id} — Delete record
 * GET  /food-schedule/rate        — JSON: fetch donation rate
 * GET  /food-schedule/search      — Filtered history search
 * GET  /food-schedule/api/item/{id} — JSON: get single item for edit modal
 * GET  /food-schedule/api/donors  — JSON: list food donors
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

    // ── Main page ─────────────────────────────────────────────────────────────

    @GetMapping
    public String listPage(Model model, Authentication authentication) {

        // Today's schedule
        List<FoodSchedule> todaySchedule = foodScheduleService.findTodaysSchedule();

        // Last 7 days grouped
        Map<LocalDate, List<FoodSchedule>> lastSevenDays = foodScheduleService.findLastSevenDays();

        // Dashboard stats
        FoodScheduleStatsDTO stats = foodScheduleService.getTodayStats();

        // Food donors for dropdown (filter strictly by donation category 'Food Donation')
        List<Donor> foodDonors = donorRepository.findAll().stream()
                .filter(d -> "Food Donation".equals(d.getDonationCategory()))
                .sorted(Comparator.comparing(Donor::getFullName))
                .toList();

        // Empty DTO for the Add form
        FoodScheduleDTO formDto = new FoodScheduleDTO();
        formDto.setScheduleDate(LocalDate.now());

        model.addAttribute("todaySchedule", todaySchedule);
        model.addAttribute("lastSevenDays", lastSevenDays);
        model.addAttribute("stats", stats);
        model.addAttribute("foodDonors", foodDonors);
        model.addAttribute("formDto", formDto);
        model.addAttribute("mealTypes", MealType.values());
        model.addAttribute("sponsorshipTypes", SponsorshipType.values());
        model.addAttribute("today", LocalDate.now());

        // Empty search results by default
        model.addAttribute("searchResults", null);
        model.addAttribute("searchPerformed", false);

        return "food-schedule/list";
    }

    // ── Save (new record) ─────────────────────────────────────────────────────

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("formDto") FoodScheduleDTO dto,
                       BindingResult result,
                       Authentication authentication,
                       RedirectAttributes redirectAttributes,
                       Model model) {

        // Additional validation: either donorId or manualDonorName must be present
        if (!dto.isDonorNotFound() && dto.getDonorId() == null) {
            result.rejectValue("donorId", "required", "Please select a donor or check 'Donor Not Found'.");
        }
        if (dto.isDonorNotFound() && (dto.getManualDonorName() == null || dto.getManualDonorName().isBlank())) {
            result.rejectValue("manualDonorName", "required", "Please enter the donor name.");
        }

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please fix the validation errors and try again.");
            return "redirect:/food-schedule";
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
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to save: " + e.getMessage());
        }

        return "redirect:/food-schedule";
    }

    // ── Update (existing record) ──────────────────────────────────────────────

    @PostMapping("/update/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("formDto") FoodScheduleDTO dto,
                         BindingResult result,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {

        if (!dto.isDonorNotFound() && dto.getDonorId() == null) {
            result.rejectValue("donorId", "required", "Please select a donor or check 'Donor Not Found'.");
        }
        if (dto.isDonorNotFound() && (dto.getManualDonorName() == null || dto.getManualDonorName().isBlank())) {
            result.rejectValue("manualDonorName", "required", "Please enter the donor name.");
        }

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please fix the validation errors and try again.");
            return "redirect:/food-schedule";
        }

        try {
            String username = authentication.getName();
            foodScheduleService.update(id, dto, username);

            auditService.logActivity(AuditModule.FOOD_SCHEDULE, AuditAction.UPDATE,
                    "Food schedule updated: ID " + id,
                    "FoodSchedule", id, true, null);

            redirectAttributes.addFlashAttribute("successMessage", "Food schedule updated successfully.");
        } catch (Exception e) {
            auditService.logActivity(AuditModule.FOOD_SCHEDULE, AuditAction.UPDATE,
                    "Failed to update food schedule: " + e.getMessage(),
                    "FoodSchedule", id, false, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update: " + e.getMessage());
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

    // ── Search history ────────────────────────────────────────────────────────

    @GetMapping("/search")
    public String search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String mealType,
            @RequestParam(required = false) String sponsorshipType,
            @RequestParam(required = false) String donorKeyword,
            Model model,
            Authentication authentication) {

        MealType mealTypeEnum = null;
        if (mealType != null && !mealType.isBlank()) {
            try { mealTypeEnum = MealType.valueOf(mealType); } catch (IllegalArgumentException ignored) {}
        }

        SponsorshipType sponsorshipTypeEnum = null;
        if (sponsorshipType != null && !sponsorshipType.isBlank()) {
            try { sponsorshipTypeEnum = SponsorshipType.valueOf(sponsorshipType); } catch (IllegalArgumentException ignored) {}
        }

        List<FoodSchedule> searchResults = foodScheduleService.searchSchedule(
                fromDate, toDate, mealTypeEnum, sponsorshipTypeEnum, donorKeyword);

        // Reload page data
        List<FoodSchedule> todaySchedule = foodScheduleService.findTodaysSchedule();
        Map<LocalDate, List<FoodSchedule>> lastSevenDays = foodScheduleService.findLastSevenDays();
        FoodScheduleStatsDTO stats = foodScheduleService.getTodayStats();

        List<Donor> foodDonors = donorRepository.findAll().stream()
                .filter(d -> "Food Donation".equals(d.getDonationCategory()))
                .sorted(Comparator.comparing(Donor::getFullName))
                .toList();

        FoodScheduleDTO formDto = new FoodScheduleDTO();
        formDto.setScheduleDate(LocalDate.now());

        model.addAttribute("todaySchedule", todaySchedule);
        model.addAttribute("lastSevenDays", lastSevenDays);
        model.addAttribute("stats", stats);
        model.addAttribute("foodDonors", foodDonors);
        model.addAttribute("formDto", formDto);
        model.addAttribute("mealTypes", MealType.values());
        model.addAttribute("sponsorshipTypes", SponsorshipType.values());
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("searchResults", searchResults);
        model.addAttribute("searchPerformed", true);

        // Retain search filters in the view
        model.addAttribute("srFromDate", fromDate);
        model.addAttribute("srToDate", toDate);
        model.addAttribute("srMealType", mealType);
        model.addAttribute("srSponsorshipType", sponsorshipType);
        model.addAttribute("srDonorKeyword", donorKeyword);

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
}
