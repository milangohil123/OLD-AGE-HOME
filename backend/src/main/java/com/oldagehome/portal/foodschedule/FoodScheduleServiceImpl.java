package com.oldagehome.portal.foodschedule;

import com.oldagehome.portal.donor.Donor;
import com.oldagehome.portal.donor.DonorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class FoodScheduleServiceImpl implements FoodScheduleService {

    private final FoodScheduleRepository foodScheduleRepository;
    private final FoodDonationRateRepository rateRepository;
    private final DonorRepository donorRepository;

    @Autowired
    public FoodScheduleServiceImpl(FoodScheduleRepository foodScheduleRepository,
                                    FoodDonationRateRepository rateRepository,
                                    DonorRepository donorRepository) {
        this.foodScheduleRepository = foodScheduleRepository;
        this.rateRepository = rateRepository;
        this.donorRepository = donorRepository;
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Override
    public FoodSchedule save(FoodScheduleDTO dto, String username) {
        // Enforce maximum 3 meals per day: only one of Breakfast, Lunch, Dinner per date
        boolean mealExists = foodScheduleRepository.findByScheduleDateOrderByServingTimeAsc(dto.getScheduleDate()).stream()
                .anyMatch(fs -> fs.getMealType() == dto.getMealType());
        if (mealExists) {
            throw new RuntimeException("A " + dto.getMealType().getDisplayName() + " has already been scheduled for " + dto.getScheduleDate() + ".");
        }

        FoodSchedule entity = toEntity(dto);
        entity.setCreatedBy(username);
        entity.setUpdatedBy(username);
        return foodScheduleRepository.save(entity);
    }

    @Override
    public FoodSchedule update(Long id, FoodScheduleDTO dto, String username) {
        // Enforce maximum 3 meals per day: only one of Breakfast, Lunch, Dinner per date (excluding current record)
        boolean mealExists = foodScheduleRepository.findByScheduleDateOrderByServingTimeAsc(dto.getScheduleDate()).stream()
                .anyMatch(fs -> fs.getMealType() == dto.getMealType() && !fs.getId().equals(id));
        if (mealExists) {
            throw new RuntimeException("A " + dto.getMealType().getDisplayName() + " has already been scheduled for " + dto.getScheduleDate() + ".");
        }

        FoodSchedule existing = foodScheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Food schedule not found: " + id));

        // Update fields
        existing.setScheduleDate(dto.getScheduleDate());
        existing.setMealType(dto.getMealType());
        existing.setServingTime(dto.getServingTime());
        existing.setMenuItems(dto.getMenuItems());
        existing.setSponsorshipType(dto.getSponsorshipType());
        existing.setAmount(dto.getAmount());
        existing.setNotes(dto.getNotes());
        existing.setUpdatedBy(username);

        // Resolve donor
        if (dto.isDonorNotFound() || dto.getDonorId() == null) {
            existing.setDonor(null);
            existing.setManualDonorName(dto.getManualDonorName());
        } else {
            Donor donor = donorRepository.findById(dto.getDonorId()).orElse(null);
            existing.setDonor(donor);
            existing.setManualDonorName(null);
        }

        return foodScheduleRepository.save(existing);
    }

    @Override
    public void delete(Long id, String username) {
        FoodSchedule entity = foodScheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Food schedule not found: " + id));
        foodScheduleRepository.delete(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FoodSchedule> findById(Long id) {
        return foodScheduleRepository.findById(id);
    }

    // ── Schedule Queries ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<FoodSchedule> findTodaysSchedule() {
        return foodScheduleRepository.findByScheduleDateOrderByServingTimeAsc(LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<LocalDate, List<FoodSchedule>> findLastSevenDays() {
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysAgo = today.minusDays(7);

        List<FoodSchedule> records = foodScheduleRepository
                .findByScheduleDateBetweenOrderByScheduleDateDescServingTimeAsc(sevenDaysAgo, today.minusDays(1));

        // Group by date, keeping insertion order (already DESC from query)
        LinkedHashMap<LocalDate, List<FoodSchedule>> grouped = new LinkedHashMap<>();
        for (FoodSchedule fs : records) {
            grouped.computeIfAbsent(fs.getScheduleDate(), k -> new ArrayList<>()).add(fs);
        }
        return grouped;
    }

    // ── Search ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<FoodSchedule> searchSchedule(LocalDate fromDate,
                                               LocalDate toDate,
                                               MealType mealType,
                                               SponsorshipType sponsorshipType,
                                               String donorKeyword) {
        String keyword = (donorKeyword != null && donorKeyword.isBlank()) ? null : donorKeyword;
        return foodScheduleRepository.searchSchedule(fromDate, toDate, mealType, sponsorshipType, keyword);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FoodSchedule> searchSchedulePaged(LocalDate fromDate,
                                                   LocalDate toDate,
                                                   MealType mealType,
                                                   SponsorshipType sponsorshipType,
                                                   String donorKeyword,
                                                   BigDecimal minAmount,
                                                   BigDecimal maxAmount,
                                                   Pageable pageable) {
        String keyword = (donorKeyword != null && donorKeyword.isBlank()) ? null : donorKeyword;
        return foodScheduleRepository.searchSchedulePaged(
                fromDate, toDate, mealType, sponsorshipType, keyword, minAmount, maxAmount, pageable);
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public FoodScheduleStatsDTO getTodayStats() {
        LocalDate today = LocalDate.now();

        long mealsCount = foodScheduleRepository.countByScheduleDate(today);
        long donorsCount = foodScheduleRepository.countDistinctDonorsByDate(today);
        BigDecimal totalAmount = foodScheduleRepository.sumAmountByDate(today);
        long upcomingCount = foodScheduleRepository.countUpcomingMeals(today);

        return new FoodScheduleStatsDTO(
                mealsCount,
                donorsCount,
                totalAmount != null ? totalAmount : BigDecimal.ZERO,
                upcomingCount,
                "", "", 0L, 0L, 0L, BigDecimal.ZERO
        );
    }

    @Override
    @Transactional(readOnly = true)
    public FoodScheduleStatsDTO getEnrichedStats() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // ── Existing KPIs ─────────────────────────────────────────────────────
        long mealsCount = foodScheduleRepository.countByScheduleDate(today);
        long donorsCount = foodScheduleRepository.countDistinctDonorsByDate(today);
        BigDecimal totalAmount = foodScheduleRepository.sumAmountByDate(today);
        long upcomingCount = foodScheduleRepository.countUpcomingMeals(today);

        // ── Next Meal KPI ─────────────────────────────────────────────────────
        List<FoodSchedule> nextMeals = foodScheduleRepository.findNextUpcomingMeals(
                today, now, PageRequest.of(0, 1));
        String nextMealName = "";
        String nextMealTime = "";
        if (!nextMeals.isEmpty()) {
            FoodSchedule next = nextMeals.get(0);
            nextMealName = next.getMealType().getDisplayName();
            nextMealTime = next.getServingTime().format(DateTimeFormatter.ofPattern("hh:mm a"));
        }

        // ── Today's Menu Item Count ───────────────────────────────────────────
        List<FoodSchedule> todayMeals = foodScheduleRepository.findByScheduleDateOrderByServingTimeAsc(today);
        long menuItemCount = todayMeals.stream()
                .filter(fs -> fs.getMenuItems() != null && !fs.getMenuItems().isBlank())
                .mapToLong(fs -> Arrays.stream(fs.getMenuItems().split("[\\n,]"))
                        .filter(line -> !line.isBlank())
                        .count())
                .sum();

        // ── Weekly Analytics ──────────────────────────────────────────────────
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd   = today.with(DayOfWeek.SUNDAY);
        long mealsThisWeek     = foodScheduleRepository.countByScheduleDateBetween(weekStart, weekEnd);
        long sponsorsThisWeek  = foodScheduleRepository.countDistinctDonorsBetween(weekStart, weekEnd);

        // ── Monthly Analytics ─────────────────────────────────────────────────
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd   = today.withDayOfMonth(today.lengthOfMonth());
        BigDecimal donationsThisMonth = foodScheduleRepository.sumAmountBetween(monthStart, monthEnd);

        return FoodScheduleStatsDTO.builder()
                .todayMealsCount(mealsCount)
                .todayFoodDonorsCount(donorsCount)
                .todayDonationAmount(totalAmount != null ? totalAmount : BigDecimal.ZERO)
                .upcomingMealsCount(upcomingCount)
                .nextScheduledMeal(nextMealName)
                .nextMealTime(nextMealTime)
                .todayMenuItemCount(menuItemCount)
                .mealsThisWeek(mealsThisWeek)
                .sponsorsThisWeek(sponsorsThisWeek)
                .donationsThisMonth(donationsThisMonth != null ? donationsThisMonth : BigDecimal.ZERO)
                .build();
    }

    // ── Rate Lookup ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Optional<BigDecimal> getRateAmount(String mealType, String sponsorshipType) {
        return rateRepository
                .findByMealTypeAndSponsorshipTypeAndIsActiveTrue(mealType, sponsorshipType)
                .map(FoodDonationRate::getAmount);
    }

    // ── Helper: DTO → Entity ──────────────────────────────────────────────────

    private FoodSchedule toEntity(FoodScheduleDTO dto) {
        FoodSchedule entity = new FoodSchedule();
        entity.setScheduleDate(dto.getScheduleDate());
        entity.setMealType(dto.getMealType());
        entity.setServingTime(dto.getServingTime());
        entity.setMenuItems(dto.getMenuItems());
        entity.setSponsorshipType(dto.getSponsorshipType());
        entity.setAmount(dto.getAmount());
        entity.setNotes(dto.getNotes());

        if (dto.isDonorNotFound() || dto.getDonorId() == null) {
            entity.setDonor(null);
            entity.setManualDonorName(dto.getManualDonorName());
        } else {
            Donor donor = donorRepository.findById(dto.getDonorId()).orElse(null);
            entity.setDonor(donor);
            entity.setManualDonorName(null);
        }

        return entity;
    }
}
