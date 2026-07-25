package com.oldagehome.portal.foodschedule;

import com.oldagehome.portal.donor.Donor;
import com.oldagehome.portal.donor.DonorRepository;
import com.oldagehome.portal.foodschedule.dto.FoodDashboardDTO;
import com.oldagehome.portal.foodschedule.dto.FoodScheduleDTO;
import com.oldagehome.portal.foodschedule.dto.FoodSponsorDTO;
import com.oldagehome.portal.foodschedule.dto.SearchDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

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
        }

        return dto;
    }
}
