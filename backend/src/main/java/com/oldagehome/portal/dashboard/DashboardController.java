package com.oldagehome.portal.dashboard;

import com.oldagehome.portal.donor.DonorService;
import com.oldagehome.portal.resident.ResidentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DonorService donorService;
    private final ResidentService residentService;

    @Autowired
    public DashboardController(DonorService donorService, ResidentService residentService) {
        this.donorService = donorService;
        this.residentService = residentService;
    }

    @GetMapping({ "/", "/dashboard" })
    public String dashboard(Model model) {
        // Resident stats
        model.addAttribute("totalResidents", residentService.getAllResidents().size());

        // Donor stats
        model.addAttribute("totalDonors", donorService.countTotalDonors());
        model.addAttribute("todayDonations", donorService.countTodayDonations());
        model.addAttribute("monthDonations", donorService.countThisMonthDonations());
        model.addAttribute("totalDonationAmount", donorService.sumTotalDonationAmount());

        return "dashboard";
    }
}
