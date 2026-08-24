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
import org.springframework.security.access.prepost.PreAuthorize;
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
            com.oldagehome.portal.foodschedule.dto.FoodDashboardDTO stats = foodScheduleService.getDashboardStats();
            model.addAttribute("dashboard", stats);
            
            // Calculate trends vs yesterday
            java.time.LocalDate yesterday = java.time.LocalDate.now().minusDays(1);
            com.oldagehome.portal.foodschedule.dto.FoodDashboardDTO prevStats = foodScheduleService.getDashboardStatsForDate(yesterday);
            
            double todaysMealsTrendVal = prevStats.getTodaysMealsCount() > 0 ? ((double) (stats.getTodaysMealsCount() - prevStats.getTodaysMealsCount()) / prevStats.getTodaysMealsCount()) * 100 : 0.0;
            model.addAttribute("prevTodaysMeals", prevStats.getTodaysMealsCount());
            model.addAttribute("todaysMealsTrend", String.format("%.2f", Math.abs(todaysMealsTrendVal)));
            model.addAttribute("todaysMealsTrendUp", todaysMealsTrendVal >= 0);

            double breakfastTrendVal = prevStats.getTodayBreakfastCount() > 0 ? ((double) (stats.getTodayBreakfastCount() - prevStats.getTodayBreakfastCount()) / prevStats.getTodayBreakfastCount()) * 100 : 0.0;
            model.addAttribute("prevBreakfast", prevStats.getTodayBreakfastCount());
            model.addAttribute("breakfastTrendValStr", String.format("%.2f", Math.abs(breakfastTrendVal)));
            model.addAttribute("breakfastTrendUp", breakfastTrendVal >= 0);

            double lunchTrendVal = prevStats.getTodayLunchCount() > 0 ? ((double) (stats.getTodayLunchCount() - prevStats.getTodayLunchCount()) / prevStats.getTodayLunchCount()) * 100 : 0.0;
            model.addAttribute("prevLunch", prevStats.getTodayLunchCount());
            model.addAttribute("lunchTrendValStr", String.format("%.2f", Math.abs(lunchTrendVal)));
            model.addAttribute("lunchTrendUp", lunchTrendVal >= 0);

            double dinnerTrendVal = prevStats.getTodayDinnerCount() > 0 ? ((double) (stats.getTodayDinnerCount() - prevStats.getTodayDinnerCount()) / prevStats.getTodayDinnerCount()) * 100 : 0.0;
            model.addAttribute("prevDinner", prevStats.getTodayDinnerCount());
            model.addAttribute("dinnerTrendValStr", String.format("%.2f", Math.abs(dinnerTrendVal)));
            model.addAttribute("dinnerTrendUp", dinnerTrendVal >= 0);

            // Active Food Sponsors trend vs yesterday
            double activeSponsorsTrendVal = prevStats.getActiveFoodSponsorsCount() > 0
                    ? ((double) (stats.getActiveFoodSponsorsCount() - prevStats.getActiveFoodSponsorsCount()) / prevStats.getActiveFoodSponsorsCount()) * 100
                    : (stats.getActiveFoodSponsorsCount() > 0 ? 100.0 : 0.0);
            model.addAttribute("prevActiveSponsors", prevStats.getActiveFoodSponsorsCount());
            model.addAttribute("activeSponsorsTrend", String.format("%.2f", Math.abs(activeSponsorsTrendVal)));
            model.addAttribute("activeSponsorsTrendUp", activeSponsorsTrendVal >= 0);

            // Upcoming Schedules trend vs yesterday
            double upcomingTrendVal = prevStats.getUpcomingSchedulesCount() > 0
                    ? ((double) (stats.getUpcomingSchedulesCount() - prevStats.getUpcomingSchedulesCount()) / prevStats.getUpcomingSchedulesCount()) * 100
                    : (stats.getUpcomingSchedulesCount() > 0 ? 100.0 : 0.0);
            model.addAttribute("prevUpcoming", prevStats.getUpcomingSchedulesCount());
            model.addAttribute("upcomingTrend", String.format("%.2f", Math.abs(upcomingTrendVal)));
            model.addAttribute("upcomingTrendUp", upcomingTrendVal >= 0);

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
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
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
