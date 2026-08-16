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
        model.addAttribute("totalResidents", residentService.countTotalResidents());

        // Donor stats
        model.addAttribute("totalDonors", donorService.countTotalDonors());
        model.addAttribute("todayDonations", donorService.countTodayDonations());
        model.addAttribute("monthDonations", donorService.countThisMonthDonations());
        model.addAttribute("totalDonationAmount", donorService.sumTotalDonationAmount());

        // Graph data
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(29);
        try {
            String trendJson = objectMapper.writeValueAsString(donorService.getDonationTrend(startDate));
            model.addAttribute("donationTrendJson", trendJson);
        } catch (JsonProcessingException e) {
            model.addAttribute("donationTrendJson", "[]");
        }

        return "dashboard";
    }
}
