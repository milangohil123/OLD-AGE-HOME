package com.oldagehome.portal.donor;

import com.oldagehome.portal.dto.DonorImportDTO;
import com.oldagehome.portal.excel.DonorExcelExporter;
import com.oldagehome.portal.excel.DonorExcelImporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class DonorServiceImpl implements DonorService {

    private final DonorRepository donorRepository;
    private final com.oldagehome.portal.audit.AuditService auditService;

    @Autowired
    public DonorServiceImpl(DonorRepository donorRepository,
                            com.oldagehome.portal.audit.AuditService auditService) {
        this.donorRepository = donorRepository;
        this.auditService = auditService;
    }

    // -------------------------------------------------------------------------
    // Core CRUD
    // -------------------------------------------------------------------------

    @Override
    public Page<Donor> getDonors(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return donorRepository.searchDonors(keyword.trim(), pageable);
        }
        return donorRepository.findAll(pageable);
    }

    @Override
    public List<Donor> getAllDonors() {
        return donorRepository.findAll();
    }

    @Override
    public Donor getDonorById(Long id) {
        return donorRepository.findById(id)
                .orElseThrow(() -> {
                    auditService.logActivity(com.oldagehome.portal.audit.AuditModule.DONOR, com.oldagehome.portal.audit.AuditAction.VIEW, "Failed to view donor: not found", "Donor", id, false, "Donor not found");
                    return new RuntimeException("Donor not found with id: " + id);
                });
    }

    @Override
    public Donor saveDonor(Donor donor) {
        Donor saved = donorRepository.save(donor);
        auditService.logActivity(com.oldagehome.portal.audit.AuditModule.DONOR, com.oldagehome.portal.audit.AuditAction.CREATE, "Created donor record for: " + saved.getFullName(), "Donor", saved.getId(), true, null);
        auditService.logActivity(com.oldagehome.portal.audit.AuditModule.DONATION, com.oldagehome.portal.audit.AuditAction.CREATE, "Created donation record for: " + saved.getFullName() + ", Amount: ₹" + saved.getDonationAmount(), "Donation", saved.getId(), true, null);
        return saved;
    }

    @Override
    public Donor updateDonor(Donor donor) {
        Donor existing = getDonorById(donor.getId());

        // Merge all editable fields
        existing.setFullName(donor.getFullName());
        existing.setGender(donor.getGender());
        existing.setDateOfBirth(donor.getDateOfBirth());
        existing.setMobile(donor.getMobile());
        existing.setEmail(donor.getEmail());
        existing.setAddress(donor.getAddress());
        existing.setCity(donor.getCity());
        existing.setState(donor.getState());
        existing.setPincode(donor.getPincode());
        existing.setDonationType(donor.getDonationType());
        existing.setDonationAmount(donor.getDonationAmount());
        existing.setDonationDate(donor.getDonationDate());
        existing.setPaymentMethod(donor.getPaymentMethod());
        existing.setTransactionId(donor.getTransactionId());
        existing.setRemarks(donor.getRemarks());
        existing.setStatus(donor.getStatus());

        // Only update photo if a new one is provided
        if (donor.getPhoto() != null && !donor.getPhoto().isBlank()) {
            existing.setPhoto(donor.getPhoto());
        }

        Donor updated = donorRepository.save(existing);
        auditService.logActivity(com.oldagehome.portal.audit.AuditModule.DONOR, com.oldagehome.portal.audit.AuditAction.UPDATE, "Updated donor record for: " + updated.getFullName(), "Donor", updated.getId(), true, null);
        return updated;
    }

    @Override
    public void deleteDonor(Long id) {
        Donor existing = getDonorById(id);
        donorRepository.delete(existing);
        auditService.logActivity(com.oldagehome.portal.audit.AuditModule.DONOR, com.oldagehome.portal.audit.AuditAction.DELETE, "Deleted donor record of: " + existing.getFullName(), "Donor", id, true, null);
    }



    // -------------------------------------------------------------------------
    // Excel Import
    // -------------------------------------------------------------------------

    @Override
    public List<DonorImportDTO> importFromExcel(MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Excel file is empty.");
        }

        List<DonorImportDTO> dtos;
        try (InputStream is = file.getInputStream()) {
            dtos = DonorExcelImporter.importDonors(is);
        }

        int successCount = 0;
        int failCount = 0;

        for (DonorImportDTO dto : dtos) {
            if (!dto.isValid()) {
                failCount++;
                continue;
            }

            try {
                Donor donor = Donor.builder()
                        .fullName(dto.getFullName())
                        .gender(dto.getGender())
                        .dateOfBirth(LocalDate.of(1980, 1, 1)) // Default DOB for imported records
                        .mobile(dto.getMobile())
                        .email(dto.getEmail())
                        .donationType(dto.getDonationType())
                        .donationAmount(dto.getDonationAmount())
                        .donationDate(dto.getDonationDate() != null ? dto.getDonationDate() : LocalDate.now())
                        .paymentMethod(dto.getPaymentMethod())
                        .status(dto.getStatus() != null ? dto.getStatus() : DonorStatus.ACTIVE)
                        .build();

                donorRepository.save(donor);
                successCount++;
            } catch (Exception e) {
                dto.setValid(false);
                dto.setErrorMessage("Failed to save: " + e.getMessage());
                failCount++;
            }
        }

        auditService.logActivity(com.oldagehome.portal.audit.AuditModule.DONOR, com.oldagehome.portal.audit.AuditAction.IMPORT, 
            "Imported donors from Excel. Success: " + successCount + ", Failed: " + failCount, "Donor", null, true, null);

        return dtos;
    }

    // -------------------------------------------------------------------------
    // Excel Export
    // -------------------------------------------------------------------------

    @Override
    public byte[] exportToExcel() throws IOException {
        List<Donor> donors = getAllDonors();
        return DonorExcelExporter.exportDonors(donors);
    }

    // -------------------------------------------------------------------------
    // Dashboard Statistics
    // -------------------------------------------------------------------------

    @Override
    public long countTotalDonors() {
        return donorRepository.count();
    }

    @Override
    public long countTodayDonations() {
        return donorRepository.countByDonationDate(LocalDate.now());
    }

    @Override
    public long countThisMonthDonations() {
        LocalDate now = LocalDate.now();
        return donorRepository.countDonationsByMonth(now.getYear(), now.getMonthValue());
    }

    @Override
    public BigDecimal sumTotalDonationAmount() {
        BigDecimal total = donorRepository.sumTotalDonationAmount();
        return total != null ? total : BigDecimal.ZERO;
    }
}
