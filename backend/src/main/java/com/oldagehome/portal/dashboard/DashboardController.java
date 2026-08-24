package com.oldagehome.portal.dashboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oldagehome.portal.donor.Donor;
import com.oldagehome.portal.donor.DonorService;
import com.oldagehome.portal.resident.ResidentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

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

    private LocalDate getCompareDate(LocalDate baseDate, int periodDays, String compareType) {
        if ("previous_period".equals(compareType)) {
            return baseDate.minusDays(periodDays);
        } else if ("1_year_earlier".equals(compareType)) {
            return baseDate.minusYears(1);
        } else {
            // Default: 1 month earlier
            return baseDate.minusMonths(1);
        }
    }

    private String getPeriodLabel(int period) {
        if (period == 7) return "Last 7 days";
        if (period == 30) return "Last 30 days";
        if (period == 90) return "Last 90 days";
        if (period == 365) return "Last 1 year";
        return "Last " + period + " days";
    }

    private String getCompareLabel(String compare) {
        if ("previous_period".equals(compare)) return "Previous period";
        if ("1_year_earlier".equals(compare)) return "1 year earlier";
        return "1 month earlier";
    }

    @GetMapping({ "/", "/dashboard" })
    public String dashboard(
            @RequestParam(value = "period", defaultValue = "30") int period,
            @RequestParam(value = "compare", defaultValue = "1_month_earlier") String compare,
            Model model) {
        
        LocalDate now = LocalDate.now();
        LocalDate periodStart = now.minusDays(period);
        LocalDate compareStart = getCompareDate(now, period, compare).minusDays(period); // Start of comparison period

        // Resident stats
        long currentResidents = residentService.countTotalResidents();
        long prevResidents = residentService.countTotalResidentsBefore(now.minusDays(period)); // Total before period start
        double residentTrend = prevResidents > 0 ? ((double) (currentResidents - prevResidents) / prevResidents) * 100 : 0.0;
        
        model.addAttribute("totalResidents", currentResidents);
        model.addAttribute("prevResidents", prevResidents);
        model.addAttribute("residentTrend", String.format("%.2f", Math.abs(residentTrend)));
        model.addAttribute("residentTrendUp", residentTrend >= 0);

        // Donor stats
        long currentDonors = donorService.countTotalDonors();
        long prevDonors = donorService.countTotalDonorsBefore(now.minusDays(period)); // Total before period start
        double donorTrend = prevDonors > 0 ? ((double) (currentDonors - prevDonors) / prevDonors) * 100 : 0.0;
        
        model.addAttribute("totalDonors", currentDonors);
        model.addAttribute("prevDonors", prevDonors);
        model.addAttribute("donorTrend", String.format("%.2f", Math.abs(donorTrend)));
        model.addAttribute("donorTrendUp", donorTrend >= 0);

        // Donations in Period (was Month Donations)
        long currentPeriodDonations = donorService.countDonationsBetween(periodStart, now);
        LocalDate compareEnd = getCompareDate(now, period, compare);
        long comparePeriodDonations = donorService.countDonationsBetween(compareStart, compareEnd);
        
        double donationPeriodTrend = comparePeriodDonations > 0 ? ((double) (currentPeriodDonations - comparePeriodDonations) / comparePeriodDonations) * 100 : 0.0;
        
        model.addAttribute("monthDonations", currentPeriodDonations);
        model.addAttribute("prevMonthDonations", comparePeriodDonations);
        model.addAttribute("donationMonthTrend", String.format("%.2f", Math.abs(donationPeriodTrend)));
        model.addAttribute("donationMonthTrendUp", donationPeriodTrend >= 0);

        // Total Donation Amount
        BigDecimal totalDonationAmount = donorService.sumTotalDonationAmount();
        BigDecimal prevAmount = donorService.sumTotalDonationAmountBefore(now.minusDays(period));
        double amountTrend = 0.0;
        if (prevAmount != null && prevAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = totalDonationAmount.subtract(prevAmount);
            amountTrend = diff.divide(prevAmount, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).doubleValue();
        }
        
        model.addAttribute("totalDonationAmount", totalDonationAmount);
        model.addAttribute("prevMonthAmount", prevAmount != null ? prevAmount : BigDecimal.ZERO);
        model.addAttribute("amountTrend", String.format("%.2f", Math.abs(amountTrend)));
        model.addAttribute("amountTrendUp", amountTrend >= 0);

        model.addAttribute("period", period);
        model.addAttribute("compare", compare);
        model.addAttribute("periodLabel", getPeriodLabel(period));
        model.addAttribute("compareLabel", getCompareLabel(compare));

        // Graph data
        try {
            List<Donor> donors = donorService.getAllDonors();
            Map<LocalDate, Long> donorCounts = new HashMap<>();
            Map<LocalDate, BigDecimal> fundCounts = new HashMap<>();

            for (Donor d : donors) {
                LocalDate dDate = d.getCreatedAt() != null ? d.getCreatedAt().toLocalDate() : d.getDonationDate();
                if (dDate != null && !dDate.isBefore(periodStart) && !dDate.isAfter(now)) {
                    donorCounts.put(dDate, donorCounts.getOrDefault(dDate, 0L) + 1);
                    if (d.getDonationAmount() != null) {
                        fundCounts.put(dDate, fundCounts.getOrDefault(dDate, BigDecimal.ZERO).add(d.getDonationAmount()));
                    }
                }
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM d");
            
            List<Map<String, Object>> donorsGraphData = new ArrayList<>();
            List<Map<String, Object>> fundsGraphData = new ArrayList<>();

            // Group by month if period is very long (e.g., 365)
            if (period > 90) {
                DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMM yyyy");
                Map<String, Long> donorMonthCounts = new LinkedHashMap<>();
                Map<String, BigDecimal> fundsMonthCounts = new LinkedHashMap<>();
                
                for (int i = 11; i >= 0; i--) {
                    String label = now.minusMonths(i).format(monthFmt);
                    donorMonthCounts.put(label, 0L);
                    fundsMonthCounts.put(label, BigDecimal.ZERO);
                }
                
                for (Donor d : donors) {
                    LocalDate dDate = d.getCreatedAt() != null ? d.getCreatedAt().toLocalDate() : d.getDonationDate();
                    if (dDate != null && !dDate.isBefore(now.minusMonths(11).withDayOfMonth(1))) {
                        String mLabel = dDate.format(monthFmt);
                        if (donorMonthCounts.containsKey(mLabel)) {
                            donorMonthCounts.put(mLabel, donorMonthCounts.get(mLabel) + 1);
                            if (d.getDonationAmount() != null) {
                                fundsMonthCounts.put(mLabel, fundsMonthCounts.get(mLabel).add(d.getDonationAmount()));
                            }
                        }
                    }
                }

                for (Map.Entry<String, Long> entry : donorMonthCounts.entrySet()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("label", entry.getKey());
                    item.put("value", entry.getValue());
                    donorsGraphData.add(item);
                }
                for (Map.Entry<String, BigDecimal> entry : fundsMonthCounts.entrySet()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("label", entry.getKey());
                    item.put("value", entry.getValue());
                    fundsGraphData.add(item);
                }
            } else {
                for (int i = period - 1; i >= 0; i--) {
                    LocalDate d = now.minusDays(i);
                    Map<String, Object> dItem = new HashMap<>();
                    dItem.put("label", d.format(dtf));
                    dItem.put("value", donorCounts.getOrDefault(d, 0L));
                    donorsGraphData.add(dItem);

                    Map<String, Object> fItem = new HashMap<>();
                    fItem.put("label", d.format(dtf));
                    fItem.put("value", fundCounts.getOrDefault(d, BigDecimal.ZERO));
                    fundsGraphData.add(fItem);
                }
            }

            model.addAttribute("donorGraphData", donorsGraphData);
            model.addAttribute("fundsGraphData", fundsGraphData);
        } catch (Exception e) {
            model.addAttribute("donorGraphData", new ArrayList<>());
            model.addAttribute("fundsGraphData", new ArrayList<>());
        }

        return "dashboard";
    }

    @GetMapping("/dashboard/export")
    public ResponseEntity<byte[]> exportDashboard(
            @RequestParam(value = "period", defaultValue = "30") int period,
            @RequestParam(value = "compare", defaultValue = "1_month_earlier") String compare) {
        
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("Dashboard Export\n");
        csvBuilder.append("Period:,").append(getPeriodLabel(period)).append("\n");
        csvBuilder.append("Compare:,").append(getCompareLabel(compare)).append("\n\n");
        
        LocalDate now = LocalDate.now();
        LocalDate periodStart = now.minusDays(period);

        csvBuilder.append("KPIs\n");
        csvBuilder.append("Metric,Current,Previous\n");
        long currentResidents = residentService.countTotalResidents();
        long prevResidents = residentService.countTotalResidentsBefore(now.minusDays(period));
        csvBuilder.append("Total Residents,").append(currentResidents).append(",").append(prevResidents).append("\n");
        
        long currentDonors = donorService.countTotalDonors();
        long prevDonors = donorService.countTotalDonorsBefore(now.minusDays(period));
        csvBuilder.append("Total Donors,").append(currentDonors).append(",").append(prevDonors).append("\n");

        long currentPeriodDonations = donorService.countDonationsBetween(periodStart, now);
        LocalDate compareStart = getCompareDate(now, period, compare).minusDays(period);
        LocalDate compareEnd = getCompareDate(now, period, compare);
        long comparePeriodDonations = donorService.countDonationsBetween(compareStart, compareEnd);
        csvBuilder.append("Donations in Period,").append(currentPeriodDonations).append(",").append(comparePeriodDonations).append("\n");
        
        BigDecimal totalDonationAmount = donorService.sumTotalDonationAmount();
        BigDecimal prevAmount = donorService.sumTotalDonationAmountBefore(now.minusDays(period));
        csvBuilder.append("Total Donation Amount,").append(totalDonationAmount).append(",").append(prevAmount != null ? prevAmount : 0).append("\n\n");
        
        csvBuilder.append("Daily Trend (Last ").append(period).append(" Days)\n");
        csvBuilder.append("Date,New Donors,Funds Received\n");
        
        try {
            List<Donor> donors = donorService.getAllDonors();
            Map<LocalDate, Long> donorCounts = new HashMap<>();
            Map<LocalDate, BigDecimal> fundCounts = new HashMap<>();

            for (Donor d : donors) {
                LocalDate dDate = d.getCreatedAt() != null ? d.getCreatedAt().toLocalDate() : d.getDonationDate();
                if (dDate != null && !dDate.isBefore(periodStart) && !dDate.isAfter(now)) {
                    donorCounts.put(dDate, donorCounts.getOrDefault(dDate, 0L) + 1);
                    if (d.getDonationAmount() != null) {
                        fundCounts.put(dDate, fundCounts.getOrDefault(dDate, BigDecimal.ZERO).add(d.getDonationAmount()));
                    }
                }
            }
            
            for (int i = period - 1; i >= 0; i--) {
                LocalDate d = now.minusDays(i);
                csvBuilder.append(d.toString()).append(",");
                csvBuilder.append(donorCounts.getOrDefault(d, 0L)).append(",");
                csvBuilder.append(fundCounts.getOrDefault(d, BigDecimal.ZERO)).append("\n");
            }
        } catch (Exception e) {
            csvBuilder.append("Error generating trend data,").append(e.getMessage()).append("\n");
        }

        byte[] csvBytes = csvBuilder.toString().getBytes();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "dashboard_export_" + now + ".csv");

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvBytes);
    }
}
