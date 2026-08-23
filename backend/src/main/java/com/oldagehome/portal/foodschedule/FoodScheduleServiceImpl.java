package com.oldagehome.portal.foodschedule;

import com.oldagehome.portal.donor.Donor;
import com.oldagehome.portal.donor.DonorRepository;
import com.oldagehome.portal.foodschedule.dto.DailyScheduleGroupDTO;
import com.oldagehome.portal.foodschedule.dto.FoodDashboardDTO;
import com.oldagehome.portal.foodschedule.dto.FoodScheduleDTO;
import com.oldagehome.portal.foodschedule.dto.FoodSponsorDTO;
import com.oldagehome.portal.foodschedule.dto.SearchDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FoodScheduleServiceImpl implements FoodScheduleService {

    private final FoodScheduleRepository foodScheduleRepository;
    private final DonorRepository donorRepository;

    @Override
    @Transactional
    public FoodScheduleDTO createSchedule(FoodScheduleDTO dto) {
        validateDuplicateMeal(dto);

        FoodSchedule schedule = new FoodSchedule();
        schedule.setMealDate(dto.getMealDate());
        schedule.setMealType(dto.getMealType());
        schedule.setMenuItems(dto.getMenuItems());
        schedule.setRemarks(dto.getRemarks());
        schedule.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");

        if (dto.getDonorId() != null) {
            Donor donor = donorRepository.findById(dto.getDonorId())
                    .orElseThrow(() -> new RuntimeException("Donor not found with ID: " + dto.getDonorId()));
            schedule.setDonor(donor);
        }

        FoodSchedule saved = foodScheduleRepository.save(schedule);
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public FoodScheduleDTO updateSchedule(Long id, FoodScheduleDTO dto) {
        validateDuplicateMeal(dto);

        FoodSchedule schedule = foodScheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Food Schedule not found with ID: " + id));

        schedule.setMealDate(dto.getMealDate());
        schedule.setMealType(dto.getMealType());
        schedule.setMenuItems(dto.getMenuItems());
        schedule.setRemarks(dto.getRemarks());
        schedule.setStatus(dto.getStatus() != null ? dto.getStatus() : schedule.getStatus());

        if (dto.getDonorId() != null) {
            Donor donor = donorRepository.findById(dto.getDonorId())
                    .orElseThrow(() -> new RuntimeException("Donor not found with ID: " + dto.getDonorId()));
            schedule.setDonor(donor);
        } else {
            schedule.setDonor(null);
        }

        FoodSchedule updated = foodScheduleRepository.save(schedule);
        return mapToDTO(updated);
    }

    @Override
    @Transactional
    public void deleteSchedule(Long id) {
        if (!foodScheduleRepository.existsById(id)) {
            throw new RuntimeException("Food Schedule not found with ID: " + id);
        }
        foodScheduleRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public FoodScheduleDTO getSchedule(Long id) {
        FoodSchedule schedule = foodScheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Food Schedule not found with ID: " + id));
        return mapToDTO(schedule);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FoodScheduleDTO> searchSchedules(SearchDTO searchDTO, Pageable pageable) {
        Page<FoodSchedule> schedules = foodScheduleRepository.searchSchedules(
                searchDTO.getFromDate(),
                searchDTO.getToDate(),
                searchDTO.getMealType(),
                searchDTO.getDonorId(),
                searchDTO.getStatus(),
                pageable
        );
        return schedules.map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public FoodDashboardDTO getDashboardStats() {
        LocalDate today = LocalDate.now();
        FoodDashboardDTO stats = new FoodDashboardDTO();

        stats.setTodaysMealsCount(foodScheduleRepository.countByMealDate(today));
        stats.setTodayBreakfastCount(foodScheduleRepository.countByMealDateAndMealType(today, MealType.BREAKFAST));
        stats.setTodayLunchCount(foodScheduleRepository.countByMealDateAndMealType(today, MealType.LUNCH));
        stats.setTodayDinnerCount(foodScheduleRepository.countByMealDateAndMealType(today, MealType.DINNER));
        stats.setActiveFoodSponsorsCount(foodScheduleRepository.countActiveFoodSponsors());
        stats.setUpcomingSchedulesCount(foodScheduleRepository.countByMealDateGreaterThanEqual(today.plusDays(1)));

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodSponsorDTO> getFoodSponsors() {
        return foodScheduleRepository.findEligibleFoodSponsors();
    }

    @Override
    public void validateDuplicateMeal(FoodScheduleDTO dto) {
        boolean exists;
        if (dto.getId() == null) {
            exists = foodScheduleRepository.existsByMealDateAndMealType(dto.getMealDate(), dto.getMealType());
        } else {
            exists = foodScheduleRepository.existsByMealDateAndMealTypeAndIdNot(dto.getMealDate(), dto.getMealType(), dto.getId());
        }

        if (exists) {
            throw new IllegalArgumentException(dto.getMealType().getDisplayName() + " is already scheduled for " + dto.getMealDate());
        }
    }

    private FoodScheduleDTO mapToDTO(FoodSchedule entity) {
        FoodScheduleDTO dto = FoodScheduleDTO.builder()
                .id(entity.getId())
                .mealDate(entity.getMealDate())
                .mealType(entity.getMealType())
                .menuItems(entity.getMenuItems())
                .remarks(entity.getRemarks())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();

        if (entity.getDonor() != null) {
            dto.setDonorId(entity.getDonor().getId());
            dto.setDonorName(entity.getDonor().getFullName());
            dto.setDonorMobile(entity.getDonor().getMobile());
            dto.setDonationAmount(entity.getDonor().getDonationAmount());
            dto.setDonationDate(entity.getDonor().getDonationDate());
            dto.setPaymentMethod(entity.getDonor().getPaymentMethod());
        }

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodScheduleDTO> getTodaysSchedules() {
        LocalDate today = LocalDate.now();
        return foodScheduleRepository.findByMealDateOrderByMealTypeAsc(today)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyScheduleGroupDTO> getPast7DaysGroups() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate sevenDaysAgo = today.minusDays(7);

        List<FoodSchedule> raw = foodScheduleRepository
                .findByDateRangeOrderByDateDescMealTypeAsc(sevenDaysAgo, yesterday);

        // Group by date preserving DESC order
        Map<LocalDate, List<FoodSchedule>> byDate = new LinkedHashMap<>();
        for (FoodSchedule fs : raw) {
            byDate.computeIfAbsent(fs.getMealDate(), d -> new ArrayList<>()).add(fs);
        }

        // Ensure all 7 days are represented even if empty
        for (int i = 1; i <= 7; i++) {
            byDate.putIfAbsent(today.minusDays(i), new ArrayList<>());
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
        List<DailyScheduleGroupDTO> groups = new ArrayList<>();

        // Iterate in DESC order (yesterday first)
        byDate.entrySet().stream()
                .sorted(Map.Entry.<LocalDate, List<FoodSchedule>>comparingByKey().reversed())
                .forEach(entry -> {
                    LocalDate date = entry.getKey();
                    List<FoodScheduleDTO> dtos = entry.getValue().stream()
                            .map(this::mapToDTO)
                            .toList();

                    BigDecimal total = dtos.stream()
                            .map(d -> d.getDonationAmount() != null ? d.getDonationAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    String label = date.equals(yesterday)
                            ? "Yesterday - " + date.format(fmt)
                            : date.format(fmt);

                    groups.add(DailyScheduleGroupDTO.builder()
                            .date(date)
                            .label(label)
                            .totalMeals(dtos.size())
                            .totalAmount(total)
                            .schedules(dtos)
                            .build());
                });

        return groups;
    }

    // --- Trend Sparkline methods ---
    @Override
    public String getMealTrend(int days) {
        java.util.List<Object[]> results = foodScheduleRepository.countMealsGroupedByDate(LocalDate.now().minusDays(days - 1));
        return com.oldagehome.portal.utils.TrendUtils.generateTrendJson(results, days);
    }

    @Override
    public String getMealTrendByType(int days, MealType mealType) {
        java.util.List<Object[]> results = foodScheduleRepository.countMealsGroupedByDateAndType(LocalDate.now().minusDays(days - 1), mealType);
        return com.oldagehome.portal.utils.TrendUtils.generateTrendJson(results, days);
    }
}
