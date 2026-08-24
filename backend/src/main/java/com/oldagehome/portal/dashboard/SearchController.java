package com.oldagehome.portal.dashboard;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;

import com.oldagehome.portal.resident.ResidentService;
import com.oldagehome.portal.resident.Resident;
import com.oldagehome.portal.donor.DonorService;
import com.oldagehome.portal.donor.Donor;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SearchController {

    @Autowired
    private ResidentService residentService;

    @Autowired
    private DonorService donorService;

    static class SearchResultItem {
        public String title;
        public String subtitle;
        public String url;
        
        public SearchResultItem(String title, String subtitle, String url) {
            this.title = title;
            this.subtitle = subtitle;
            this.url = url;
        }
    }

    @GetMapping("/search")
    public List<SearchResultItem> search(@RequestParam(value = "q", defaultValue = "") String query) {
        List<SearchResultItem> results = new ArrayList<>();
        if (query == null || query.trim().length() < 2) {
            return results;
        }
        
        String q = query.trim();
        PageRequest pageRequest = PageRequest.of(0, 5); // Limit 5 of each

        try {
            Page<Resident> residents = residentService.getResidents(q, pageRequest);
            if (residents != null) {
                for (Resident r : residents.getContent()) {
                    results.add(new SearchResultItem(r.getFullName(), "Resident ID: " + (r.getId() != null ? r.getId() : ""), "/residents/" + r.getId()));
                }
            }
        } catch (Exception e) {
            // Ignore if error occurs
        }

        try {
            Page<Donor> donors = donorService.getDonors(q, pageRequest);
            if (donors != null) {
                for (Donor d : donors.getContent()) {
                    results.add(new SearchResultItem(d.getFullName(), "Donor - " + (d.getMobile() != null ? d.getMobile() : ""), "/donors/" + d.getId()));
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        return results;
    }
}
