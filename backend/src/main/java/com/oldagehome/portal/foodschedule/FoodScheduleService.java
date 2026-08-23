package com.oldagehome.portal.foodschedule;

import com.oldagehome.portal.foodschedule.dto.DailyScheduleGroupDTO;
import com.oldagehome.portal.foodschedule.dto.FoodDashboardDTO;
import com.oldagehome.portal.foodschedule.dto.FoodScheduleDTO;
import com.oldagehome.portal.foodschedule.dto.FoodSponsorDTO;
import com.oldagehome.portal.foodschedule.dto.SearchDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FoodScheduleService {
    
    FoodScheduleDTO createSchedule(FoodScheduleDTO dto);
    
    FoodScheduleDTO updateSchedule(Long id, FoodScheduleDTO dto);
    
    void deleteSchedule(Long id);
    
    FoodScheduleDTO getSchedule(Long id);
    
    Page<FoodScheduleDTO> searchSchedules(SearchDTO searchDTO, Pageable pageable);
    
    FoodDashboardDTO getDashboardStats();
    
    FoodDashboardDTO getDashboardStatsForDate(java.time.LocalDate date);
    
    List<FoodSponsorDTO> getFoodSponsors();
    
    void validateDuplicateMeal(FoodScheduleDTO dto);

    /** All schedules for today, ordered by meal type (BREAKFAST → LUNCH → DINNER). */
    List<FoodScheduleDTO> getTodaysSchedules();

    /** Schedules from yesterday back 6 more days, grouped by date with totals. */
    List<DailyScheduleGroupDTO> getPast7DaysGroups();

    // --- Trend Sparkline methods ---
    String getMealTrend(int days);

    String getMealTrendByType(int days, MealType mealType);
}
