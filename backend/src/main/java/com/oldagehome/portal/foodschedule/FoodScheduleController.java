package com.oldagehome.portal.foodschedule;

import com.oldagehome.portal.audit.AuditAction;
import com.oldagehome.portal.audit.AuditModule;
import com.oldagehome.portal.audit.AuditService;
import com.oldagehome.portal.foodschedule.dto.FoodScheduleDTO;
import com.oldagehome.portal.foodschedule.dto.SearchDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/food-schedule")
@RequiredArgsConstructor
public class FoodScheduleController {

    private final FoodScheduleService foodScheduleService;
    private final AuditService auditService;

    @GetMapping
    public String listSchedules(
            @ModelAttribute("searchDTO") SearchDTO searchDTO,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            // Handle empty strings from form submission
            if (searchDTO.getStatus() != null && searchDTO.getStatus().trim().isEmpty()) {
                searchDTO.setStatus(null);
            }
            
            Pageable pageable = PageRequest.of(page - 1, size);
            Page<FoodScheduleDTO> schedules = foodScheduleService.searchSchedules(searchDTO, pageable);

            model.addAttribute("schedules", schedules);
            model.addAttribute("dashboard", foodScheduleService.getDashboardStats());
            model.addAttribute("sponsors", foodScheduleService.getFoodSponsors());
            model.addAttribute("todaysSchedules", foodScheduleService.getTodaysSchedules());
            model.addAttribute("past7DaysGroups", foodScheduleService.getPast7DaysGroups());
            model.addAttribute("todayDate", java.time.LocalDate.now());
            model.addAttribute("activePage", "food-schedule");

            // Trend data for sparklines
            model.addAttribute("mealTrend", foodScheduleService.getMealTrend(7));
            model.addAttribute("breakfastTrend", foodScheduleService.getMealTrendByType(7, MealType.BREAKFAST));
            model.addAttribute("lunchTrend", foodScheduleService.getMealTrendByType(7, MealType.LUNCH));
            model.addAttribute("dinnerTrend", foodScheduleService.getMealTrendByType(7, MealType.DINNER));

            return "food-schedule/list";
        } catch (Exception e) {
            e.printStackTrace();
            auditService.logActivity(AuditModule.FOOD_SCHEDULE, AuditAction.VIEW,
                    "Error searching food schedules: " + e.getMessage(),
                    "FoodSchedule", null, false, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while loading food schedules.");
            return "redirect:/dashboard"; // Redirect to a safe page
        }
    }

    @PostMapping("/save")
    public String saveSchedule(@Valid @ModelAttribute("schedule") FoodScheduleDTO dto,
                               BindingResult result,
                               RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation errors occurred. Please check your input.");
            return "redirect:/food-schedule";
        }

        try {
            if (dto.getId() == null) {
                foodScheduleService.createSchedule(dto);
                auditService.logActivity(AuditModule.FOOD_SCHEDULE, AuditAction.CREATE,
                        "Created food schedule for " + dto.getMealDate(),
                        "FoodSchedule", null, true, null);
                redirectAttributes.addFlashAttribute("successMessage", "Food schedule created successfully!");
            } else {
                foodScheduleService.updateSchedule(dto.getId(), dto);
                auditService.logActivity(AuditModule.FOOD_SCHEDULE, AuditAction.UPDATE,
                        "Updated food schedule ID " + dto.getId(),
                        "FoodSchedule", dto.getId(), true, null);
                redirectAttributes.addFlashAttribute("successMessage", "Food schedule updated successfully!");
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while saving the schedule.");
        }

        return "redirect:/food-schedule";
    }

    @GetMapping("/delete/{id}")
    public String deleteSchedule(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            foodScheduleService.deleteSchedule(id);
            auditService.logActivity(AuditModule.FOOD_SCHEDULE, AuditAction.DELETE,
                    "Deleted food schedule ID " + id,
                    "FoodSchedule", id, true, null);
            redirectAttributes.addFlashAttribute("successMessage", "Food schedule deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not delete schedule. " + e.getMessage());
        }
        return "redirect:/food-schedule";
    }

    @GetMapping("/api/item/{id}")
    @ResponseBody
    public FoodScheduleDTO getScheduleApi(@PathVariable Long id) {
        return foodScheduleService.getSchedule(id);
    }
}
