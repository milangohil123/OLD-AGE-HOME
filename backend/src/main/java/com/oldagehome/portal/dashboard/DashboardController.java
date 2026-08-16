package com.oldagehome.portal.dashboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oldagehome.portal.donor.DonorService;
import com.oldagehome.portal.resident.ResidentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

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
        // Resident stats
        long currentResidents = residentService.countTotalResidents();
        long prevResidents = residentService.countTotalResidentsBefore(LocalDate.now().minusDays(30));
        double residentTrend = prevResidents > 0 ? ((double) (currentResidents - prevResidents) / prevResidents) * 100 : 0.0;
        
        model.addAttribute("totalResidents", currentResidents);
        model.addAttribute("prevResidents", prevResidents);
        model.addAttribute("residentTrend", String.format("%.2f", Math.abs(residentTrend)));
        model.addAttribute("residentTrendUp", residentTrend >= 0);

        // Donor stats
        long currentDonors = donorService.countTotalDonors();
        long prevDonors = donorService.countTotalDonorsBefore(LocalDate.now().minusDays(30));
        double donorTrend = prevDonors > 0 ? ((double) (currentDonors - prevDonors) / prevDonors) * 100 : 0.0;
        
        model.addAttribute("totalDonors", currentDonors);
        model.addAttribute("prevDonors", prevDonors);
        model.addAttribute("donorTrend", String.format("%.2f", Math.abs(donorTrend)));
        model.addAttribute("donorTrendUp", donorTrend >= 0);

        LocalDate now = LocalDate.now();
        long monthDonations = donorService.countThisMonthDonations();
        long prevMonthDonations = donorService.countDonationsBetween(
            now.minusMonths(1).withDayOfMonth(1), 
            now.minusMonths(1).withDayOfMonth(now.minusMonths(1).lengthOfMonth())
        );
        double donationMonthTrend = prevMonthDonations > 0 ? ((double) (monthDonations - prevMonthDonations) / prevMonthDonations) * 100 : 0.0;
        
        model.addAttribute("monthDonations", monthDonations);
        model.addAttribute("prevMonthDonations", prevMonthDonations);
        model.addAttribute("donationMonthTrend", String.format("%.2f", Math.abs(donationMonthTrend)));
        model.addAttribute("donationMonthTrendUp", donationMonthTrend >= 0);

        java.math.BigDecimal totalDonationAmount = donorService.sumTotalDonationAmount();
        java.math.BigDecimal prevMonthAmount = donorService.sumTotalDonationAmountBefore(now.withDayOfMonth(1));
        double amountTrend = 0.0;
        if (prevMonthAmount.compareTo(java.math.BigDecimal.ZERO) > 0) {
            java.math.BigDecimal diff = totalDonationAmount.subtract(prevMonthAmount);
            amountTrend = diff.divide(prevMonthAmount, 4, java.math.RoundingMode.HALF_UP).multiply(new java.math.BigDecimal("100")).doubleValue();
        }
        
        model.addAttribute("totalDonationAmount", totalDonationAmount);
        model.addAttribute("prevMonthAmount", prevMonthAmount);
        model.addAttribute("amountTrend", String.format("%.2f", Math.abs(amountTrend)));
        model.addAttribute("amountTrendUp", amountTrend >= 0);

        // Graph data
        try {
            java.util.Map<String, Object> graphData = new java.util.HashMap<>();
            
            java.util.List<com.oldagehome.portal.donor.Donor> donors = donorService.getAllDonors();
            java.util.Map<LocalDate, Long> donorCounts = new java.util.HashMap<>();
            for (com.oldagehome.portal.donor.Donor d : donors) {
                LocalDate dDate = d.getCreatedAt() != null ? d.getCreatedAt().toLocalDate() : d.getDonationDate();
                if (dDate != null) {
                    donorCounts.put(dDate, donorCounts.getOrDefault(dDate, 0L) + 1);
                }
            }

            java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("MMM d");
            
            // Week
            java.util.List<java.util.Map<String, Object>> weekData = new java.util.ArrayList<>();
            for (int i = 6; i >= 0; i--) {
                LocalDate d = now.minusDays(i);
                java.util.Map<String, Object> item = new java.util.HashMap<>();
                item.put("label", d.format(dtf));
                item.put("count", donorCounts.getOrDefault(d, 0L));
                weekData.add(item);
            }
            
            // Month
            java.util.List<java.util.Map<String, Object>> monthData = new java.util.ArrayList<>();
            for (int i = 29; i >= 0; i--) {
                LocalDate d = now.minusDays(i);
                java.util.Map<String, Object> item = new java.util.HashMap<>();
                item.put("label", d.format(dtf));
                item.put("count", donorCounts.getOrDefault(d, 0L));
                monthData.add(item);
            }
            
            // Year (group by month)
            java.util.List<java.util.Map<String, Object>> yearData = new java.util.ArrayList<>();
            java.time.format.DateTimeFormatter monthFmt = java.time.format.DateTimeFormatter.ofPattern("MMM yyyy");
            java.util.Map<String, Long> yearCounts = new java.util.LinkedHashMap<>();
            for (int i = 11; i >= 0; i--) {
                yearCounts.put(now.minusMonths(i).format(monthFmt), 0L);
            }
            for (com.oldagehome.portal.donor.Donor d : donors) {
                LocalDate dDate = d.getCreatedAt() != null ? d.getCreatedAt().toLocalDate() : d.getDonationDate();
                if (dDate != null && !dDate.isBefore(now.minusMonths(11).withDayOfMonth(1))) {
                    String mLabel = dDate.format(monthFmt);
                    if (yearCounts.containsKey(mLabel)) {
                        yearCounts.put(mLabel, yearCounts.get(mLabel) + 1);
                    }
                }
            }
            for (java.util.Map.Entry<String, Long> entry : yearCounts.entrySet()) {
                java.util.Map<String, Object> item = new java.util.HashMap<>();
                item.put("label", entry.getKey());
                item.put("count", entry.getValue());
                yearData.add(item);
            }

            graphData.put("week", weekData);
            graphData.put("month", monthData);
            graphData.put("year", yearData);

            model.addAttribute("donorGraphMap", graphData);
        } catch (Exception e) {
            model.addAttribute("donorGraphMap", new java.util.HashMap<>());
        }

        return "dashboard";
    }
}
