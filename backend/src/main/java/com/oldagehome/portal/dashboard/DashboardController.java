package com.oldagehome.portal.dashboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oldagehome.portal.donor.DonorService;
import com.oldagehome.portal.resident.ResidentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class DashboardController {

    private final DonorService donorService;
    private final ResidentService residentService;
    private final ObjectMapper objectMapper;

    @Autowired
    public DashboardController(DonorService donorService, ResidentService residentService, ObjectMapper objectMapper) {
        this.donorService = donorService;
        this.residentService = residentService;
        this.objectMapper = objectMapper;
    }

    @GetMapping({ "/", "/dashboard" })
    public String dashboard(Model model) {
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(30);
        LocalDateTime thirtyDaysAgoDateTime = LocalDateTime.of(thirtyDaysAgo, LocalTime.MIDNIGHT);
        LocalDate prevMonthDate = today.minusMonths(1);

        // ── KPI: Current Values ──────────────────────────────────────────────
        long totalResidents = residentService.countTotalResidents();
        long totalDonors = donorService.countTotalDonors();
        long monthDonations = donorService.countThisMonthDonations();
        BigDecimal totalDonationAmount = donorService.sumTotalDonationAmount();

        model.addAttribute("totalResidents", totalResidents);
        model.addAttribute("totalDonors", totalDonors);
        model.addAttribute("monthDonations", monthDonations);
        model.addAttribute("totalDonationAmount", totalDonationAmount);

        // ── KPI Trend: Total Residents (current vs 30 days prior) ────────────
        long prevResidents = residentService.countResidentsJoinedByDate(thirtyDaysAgo);
        String residentTrend = null;
        boolean residentTrendUp = true;
        if (prevResidents > 0) {
            double pct = (totalResidents - prevResidents) * 100.0 / prevResidents;
            residentTrend = String.format("%.2f", Math.abs(pct));
            residentTrendUp = pct >= 0;
        }
        model.addAttribute("residentTrend", residentTrend);
        model.addAttribute("residentTrendUp", residentTrendUp);
        model.addAttribute("prevResidents", prevResidents);

        // ── KPI Trend: Total Donors (current vs 30 days prior) ───────────────
        long prevDonors = donorService.countDonorsRegisteredByDateTime(thirtyDaysAgoDateTime);
        String donorTrend = null;
        boolean donorTrendUp = true;
        if (prevDonors > 0) {
            double pct = (totalDonors - prevDonors) * 100.0 / prevDonors;
            donorTrend = String.format("%.2f", Math.abs(pct));
            donorTrendUp = pct >= 0;
        }
        model.addAttribute("donorTrend", donorTrend);
        model.addAttribute("donorTrendUp", donorTrendUp);
        model.addAttribute("prevDonors", prevDonors);

        // ── KPI Trend: Donations This Month vs Previous Month ────────────────
        long prevMonthDonations = donorService.countDonationsByMonth(prevMonthDate.getYear(), prevMonthDate.getMonthValue());
        String donationMonthTrend = null;
        boolean donationMonthTrendUp = true;
        if (prevMonthDonations > 0) {
            double pct = (monthDonations - prevMonthDonations) * 100.0 / prevMonthDonations;
            donationMonthTrend = String.format("%.2f", Math.abs(pct));
            donationMonthTrendUp = pct >= 0;
        }
        model.addAttribute("donationMonthTrend", donationMonthTrend);
        model.addAttribute("donationMonthTrendUp", donationMonthTrendUp);
        model.addAttribute("prevMonthDonations", prevMonthDonations);

        // ── KPI Trend: Total Raised (this month vs previous month) ───────────
        BigDecimal thisMonthAmount = donorService.sumDonationAmountByMonth(today.getYear(), today.getMonthValue());
        BigDecimal prevMonthAmount = donorService.sumDonationAmountByMonth(prevMonthDate.getYear(), prevMonthDate.getMonthValue());
        String amountTrend = null;
        boolean amountTrendUp = true;
        if (prevMonthAmount != null && prevMonthAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal pct = thisMonthAmount.subtract(prevMonthAmount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(prevMonthAmount, 2, RoundingMode.HALF_UP);
            amountTrend = pct.abs().toPlainString();
            amountTrendUp = pct.compareTo(BigDecimal.ZERO) >= 0;
        }
        model.addAttribute("amountTrend", amountTrend);
        model.addAttribute("amountTrendUp", amountTrendUp);
        model.addAttribute("prevMonthAmount", prevMonthAmount);

        // ── Donor Graph: Week / Month / Year (all loaded upfront, JS switches) ─
        try {
            Map<String, Object> graphData = new LinkedHashMap<>();
            graphData.put("week", donorService.getDonorGraphWeek());
            graphData.put("month", donorService.getDonorGraphMonth());
            graphData.put("year", donorService.getDonorGraphYear());
            model.addAttribute("donorGraphJson", objectMapper.writeValueAsString(graphData));
        } catch (JsonProcessingException e) {
            model.addAttribute("donorGraphJson", "{\"week\":[],\"month\":[],\"year\":[]}");
        }

        return "dashboard";
    }
}
