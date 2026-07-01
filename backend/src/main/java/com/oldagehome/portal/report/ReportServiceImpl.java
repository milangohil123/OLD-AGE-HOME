package com.oldagehome.portal.report;

import com.oldagehome.portal.donor.DonorRepository;
import com.oldagehome.portal.donor.DonorStatus;
import com.oldagehome.portal.donor.DonationType;
import com.oldagehome.portal.inventory.InventoryRepository;
import com.oldagehome.portal.inventory.InventoryStatus;
import com.oldagehome.portal.inventory.MedicineCategory;
import com.oldagehome.portal.resident.Resident;
import com.oldagehome.portal.resident.ResidentRepository;
import com.oldagehome.portal.resident.ResidentStatus;
import com.oldagehome.portal.donor.Donor;
import com.oldagehome.portal.inventory.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    public Page<Resident> getResidentsReport(String status, String keyword, Integer month, Integer year, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Specification<Resident> specification = Specification.where(null);

        if (hasText(status) && !status.equalsIgnoreCase("ALL")) {
            ResidentStatus resStatus = ResidentStatus.valueOf(status.toUpperCase());
            specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), resStatus));
        }

        if (hasText(keyword)) {
            String pattern = likePattern(keyword);
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(root.get("residentId")), pattern),
                    cb.like(cb.lower(root.get("mobile")), pattern),
                    cb.like(cb.lower(root.get("guardianName")), pattern),
                    cb.like(cb.lower(root.get("guardianPhone")), pattern)
            ));
        }

        if (month != null) {
            specification = specification.and((root, query, cb) -> cb.equal(cb.function("month", Integer.class, root.get("joiningDate")), month));
        }

        if (year != null) {
            specification = specification.and((root, query, cb) -> cb.equal(cb.function("year", Integer.class, root.get("joiningDate")), year));
        }

        if (startDate != null) {
            specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("joiningDate"), startDate));
        }

        if (endDate != null) {
            specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("joiningDate"), endDate));
        }

        return residentRepository.findAll(specification, pageable);
    }

    @Override
    public Page<Donor> getDonorsReport(String status, String keyword, String donationType, Pageable pageable) {
        Specification<Donor> specification = Specification.where(null);

        if (hasText(status) && !status.equalsIgnoreCase("ALL")) {
            DonorStatus donorStatus = DonorStatus.valueOf(status.toUpperCase());
            specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), donorStatus));
        }

        if (hasText(keyword)) {
            String pattern = likePattern(keyword);
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(root.get("donorId")), pattern),
                    cb.like(cb.lower(root.get("mobile")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern)
            ));
        }

        if (hasText(donationType) && !donationType.equalsIgnoreCase("ALL")) {
            DonationType type = DonationType.valueOf(donationType.toUpperCase());
            specification = specification.and((root, query, cb) -> cb.equal(root.get("donationType"), type));
        }

        return donorRepository.findAll(specification, pageable);
    }

    @Override
    public Page<Donor> getDonationsReport(String paymentMethod, Integer month, Integer year, LocalDate startDate, LocalDate endDate, String donationType, Pageable pageable) {
        Specification<Donor> specification = Specification.where(null);

        if (hasText(paymentMethod) && !paymentMethod.equalsIgnoreCase("ALL")) {
            specification = specification.and((root, query, cb) -> cb.equal(cb.upper(root.get("paymentMethod")), paymentMethod.trim().toUpperCase()));
        }

        if (hasText(donationType) && !donationType.equalsIgnoreCase("ALL")) {
            DonationType type = DonationType.valueOf(donationType.toUpperCase());
            specification = specification.and((root, query, cb) -> cb.equal(root.get("donationType"), type));
        }

        if (month != null) {
            specification = specification.and((root, query, cb) -> cb.equal(cb.function("month", Integer.class, root.get("donationDate")), month));
        }

        if (year != null) {
            specification = specification.and((root, query, cb) -> cb.equal(cb.function("year", Integer.class, root.get("donationDate")), year));
        }

        if (startDate != null) {
            specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("donationDate"), startDate));
        }

        if (endDate != null) {
            specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("donationDate"), endDate));
        }

        return donorRepository.findAll(specification, pageable);
    }

    @Override
    public Page<Inventory> getInventoryReport(String status, String keyword, String category, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Specification<Inventory> specification = Specification.where(null);

        if (hasText(status) && !status.equalsIgnoreCase("ALL")) {
            InventoryStatus invStatus = InventoryStatus.valueOf(status.toUpperCase());
            specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), invStatus));
        }

        if (hasText(keyword)) {
            String pattern = likePattern(keyword);
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("medicineName")), pattern),
                    cb.like(cb.lower(root.get("medicineCode")), pattern),
                    cb.like(cb.lower(root.get("manufacturer")), pattern),
                    cb.like(cb.lower(root.get("supplier")), pattern),
                    cb.like(cb.lower(root.get("batchNumber")), pattern)
            ));
        }

        if (hasText(category) && !category.equalsIgnoreCase("ALL")) {
            MedicineCategory cat = MedicineCategory.valueOf(category.toUpperCase());
            specification = specification.and((root, query, cb) -> cb.equal(root.get("category"), cat));
        }

        if (startDate != null) {
            specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("purchaseDate"), startDate));
        }

        if (endDate != null) {
            specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("purchaseDate"), endDate));
        }

        return inventoryRepository.findAll(specification, pageable);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String likePattern(String value) {
        return "%" + value.trim().toLowerCase() + "%";
    }
}
