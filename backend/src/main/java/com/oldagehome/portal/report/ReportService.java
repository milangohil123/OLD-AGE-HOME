package com.oldagehome.portal.report;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.oldagehome.portal.resident.Resident;
import com.oldagehome.portal.donor.Donor;
import com.oldagehome.portal.inventory.Inventory;

import java.time.LocalDate;

public interface ReportService {

    Page<Resident> getResidentsReport(String status, String keyword, Integer month, Integer year, LocalDate startDate, LocalDate endDate, Pageable pageable);

    Page<Donor> getDonorsReport(String status, String keyword, String donationType, Pageable pageable);

    Page<Donor> getDonationsReport(String paymentMethod, Integer month, Integer year, LocalDate startDate, LocalDate endDate, String donationType, Pageable pageable);

    Page<Inventory> getInventoryReport(String status, String keyword, String category, LocalDate startDate, LocalDate endDate, Pageable pageable);
}
