package com.oldagehome.portal.report;

import com.oldagehome.portal.resident.Resident;
import com.oldagehome.portal.resident.ResidentRepository;
import com.oldagehome.portal.resident.ResidentStatus;
import com.oldagehome.portal.donor.Donor;
import com.oldagehome.portal.donor.DonorRepository;
import com.oldagehome.portal.donor.DonorStatus;
import com.oldagehome.portal.donor.DonationType;
import com.oldagehome.portal.inventory.Inventory;
import com.oldagehome.portal.inventory.InventoryRepository;
import com.oldagehome.portal.inventory.InventoryStatus;
import com.oldagehome.portal.inventory.MedicineCategory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final ResidentRepository residentRepository;
    private final DonorRepository donorRepository;
    private final InventoryRepository inventoryRepository;

    @Autowired
    public ReportServiceImpl(ResidentRepository residentRepository,
                             DonorRepository donorRepository,
                             InventoryRepository inventoryRepository) {
        this.residentRepository = residentRepository;
        this.donorRepository = donorRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public List<Resident> getResidentsReport(String status, String keyword, Integer month, Integer year, LocalDate startDate, LocalDate endDate) {
        Stream<Resident> stream = residentRepository.findAll().stream();

        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
            ResidentStatus resStatus = ResidentStatus.valueOf(status.toUpperCase());
            stream = stream.filter(r -> r.getStatus() == resStatus);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.toLowerCase();
            stream = stream.filter(r -> r.getFullName().toLowerCase().contains(kw) || 
                                        r.getResidentId().toLowerCase().contains(kw) || 
                                        (r.getMobile() != null && r.getMobile().contains(kw)));
        }

        if (month != null) {
            stream = stream.filter(r -> r.getJoiningDate() != null && r.getJoiningDate().getMonthValue() == month);
        }

        if (year != null) {
            stream = stream.filter(r -> r.getJoiningDate() != null && r.getJoiningDate().getYear() == year);
        }

        if (startDate != null) {
            stream = stream.filter(r -> r.getJoiningDate() != null && !r.getJoiningDate().isBefore(startDate));
        }

        if (endDate != null) {
            stream = stream.filter(r -> r.getJoiningDate() != null && !r.getJoiningDate().isAfter(endDate));
        }

        return stream.collect(Collectors.toList());
    }

    @Override
    public List<Donor> getDonorsReport(String status, String keyword, String donationType) {
        Stream<Donor> stream = donorRepository.findAll().stream();

        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
            DonorStatus donorStatus = DonorStatus.valueOf(status.toUpperCase());
            stream = stream.filter(d -> d.getStatus() == donorStatus);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.toLowerCase();
            stream = stream.filter(d -> d.getFullName().toLowerCase().contains(kw) || 
                                        d.getDonorId().toLowerCase().contains(kw) || 
                                        (d.getMobile() != null && d.getMobile().contains(kw)));
        }

        if (donationType != null && !donationType.trim().isEmpty() && !donationType.equalsIgnoreCase("ALL")) {
            DonationType type = DonationType.valueOf(donationType.toUpperCase());
            stream = stream.filter(d -> d.getDonationType() == type);
        }

        return stream.collect(Collectors.toList());
    }

    @Override
    public List<Donor> getDonationsReport(String paymentMethod, Integer month, Integer year, LocalDate startDate, LocalDate endDate, String donationType) {
        Stream<Donor> stream = donorRepository.findAll().stream();

        if (paymentMethod != null && !paymentMethod.trim().isEmpty() && !paymentMethod.equalsIgnoreCase("ALL")) {
            stream = stream.filter(d -> d.getPaymentMethod() != null && d.getPaymentMethod().equalsIgnoreCase(paymentMethod));
        }

        if (donationType != null && !donationType.trim().isEmpty() && !donationType.equalsIgnoreCase("ALL")) {
            DonationType type = DonationType.valueOf(donationType.toUpperCase());
            stream = stream.filter(d -> d.getDonationType() == type);
        }

        if (month != null) {
            stream = stream.filter(d -> d.getDonationDate() != null && d.getDonationDate().getMonthValue() == month);
        }

        if (year != null) {
            stream = stream.filter(d -> d.getDonationDate() != null && d.getDonationDate().getYear() == year);
        }

        if (startDate != null) {
            stream = stream.filter(d -> d.getDonationDate() != null && !d.getDonationDate().isBefore(startDate));
        }

        if (endDate != null) {
            stream = stream.filter(d -> d.getDonationDate() != null && !d.getDonationDate().isAfter(endDate));
        }

        return stream.collect(Collectors.toList());
    }

    @Override
    public List<Inventory> getInventoryReport(String status, String keyword, String category, LocalDate startDate, LocalDate endDate) {
        Stream<Inventory> stream = inventoryRepository.findAll().stream();

        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
            InventoryStatus invStatus = InventoryStatus.valueOf(status.toUpperCase());
            stream = stream.filter(i -> i.getStatus() == invStatus);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.toLowerCase();
            stream = stream.filter(i -> i.getMedicineName().toLowerCase().contains(kw) || 
                                        i.getMedicineCode().toLowerCase().contains(kw) || 
                                        (i.getManufacturer() != null && i.getManufacturer().toLowerCase().contains(kw)));
        }

        if (category != null && !category.trim().isEmpty() && !category.equalsIgnoreCase("ALL")) {
            MedicineCategory cat = MedicineCategory.valueOf(category.toUpperCase());
            stream = stream.filter(i -> i.getCategory() == cat);
        }

        if (startDate != null) {
            stream = stream.filter(i -> i.getPurchaseDate() != null && !i.getPurchaseDate().isBefore(startDate));
        }

        if (endDate != null) {
            stream = stream.filter(i -> i.getPurchaseDate() != null && !i.getPurchaseDate().isAfter(endDate));
        }

        return stream.collect(Collectors.toList());
    }
}
