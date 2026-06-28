package com.oldagehome.portal.report;

import com.oldagehome.portal.resident.Resident;
import com.oldagehome.portal.donor.Donor;
import com.oldagehome.portal.inventory.Inventory;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {

    List<Resident> getResidentsReport(String status, String keyword, Integer month, Integer year, LocalDate startDate, LocalDate endDate);

    List<Donor> getDonorsReport(String status, String keyword, String donationType);

    List<Donor> getDonationsReport(String paymentMethod, Integer month, Integer year, LocalDate startDate, LocalDate endDate, String donationType);

    List<Inventory> getInventoryReport(String status, String keyword, String category, LocalDate startDate, LocalDate endDate);
}
