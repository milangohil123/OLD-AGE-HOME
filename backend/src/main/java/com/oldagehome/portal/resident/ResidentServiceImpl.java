package com.oldagehome.portal.resident;

import com.oldagehome.portal.dto.ResidentImportDTO;
import com.oldagehome.portal.excel.ResidentExcelImporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class ResidentServiceImpl implements ResidentService {

    private final ResidentRepository residentRepository;
    private final com.oldagehome.portal.audit.AuditService auditService;

    @Autowired
    public ResidentServiceImpl(ResidentRepository residentRepository,
            com.oldagehome.portal.audit.AuditService auditService) {
        this.residentRepository = residentRepository;
        this.auditService = auditService;
    }

    @Override
    public Page<Resident> getResidents(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return residentRepository.searchResidents(keyword.trim(), pageable);
        }
        return residentRepository.findAll(pageable);
    }

    @Override
    public List<Resident> getAllResidents() {
        return residentRepository.findAll();
    }

    @Override
    public long countTotalResidents() {
        return residentRepository.count();
    }

    @Override
    public long countTotalResidentsBefore(java.time.LocalDate date) {
        return residentRepository.countByJoiningDateBefore(date);
    }

    @Override
    public List<Resident> getRecentResidents(int limit) {
        return residentRepository
                .findAll(
                        org.springframework.data.domain.PageRequest.of(0, limit,
                                org.springframework.data.domain.Sort
                                        .by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")))
                .getContent();
    }

    @Override
    public Resident getResidentById(Long id) {
        return residentRepository.findById(id)
                .orElseThrow(() -> {
                    auditService.logActivity(com.oldagehome.portal.audit.AuditModule.RESIDENT,
                            com.oldagehome.portal.audit.AuditAction.VIEW, "Failed to view resident: not found",
                            "Resident", id, false, "Resident not found");
                    return new RuntimeException("Resident not found with id: " + id);
                });
    }

    @Override
    public Resident saveResident(Resident resident) {
        Resident saved = residentRepository.save(resident);
        auditService.logActivity(com.oldagehome.portal.audit.AuditModule.RESIDENT,
                com.oldagehome.portal.audit.AuditAction.CREATE, "Created resident: " + saved.getFullName(), "Resident",
                saved.getId(), true, null);
        return saved;
    }

    @Override
    public Resident updateResident(Resident resident) {
        Resident existing = getResidentById(resident.getId());

        // Merge attributes
        existing.setFullName(resident.getFullName());
        existing.setGender(resident.getGender());
        existing.setDateOfBirth(resident.getDateOfBirth());
        existing.setBloodGroup(resident.getBloodGroup());
        existing.setMobile(resident.getMobile());
        existing.setEmail(resident.getEmail());
        existing.setAddress(resident.getAddress());
        existing.setJoiningDate(resident.getJoiningDate());
        existing.setStatus(resident.getStatus());
        existing.setGuardianName(resident.getGuardianName());
        existing.setGuardianPhone(resident.getGuardianPhone());
        existing.setGuardianEmail(resident.getGuardianEmail());
        existing.setGuardianAddress(resident.getGuardianAddress());
        existing.setMedicalNotes(resident.getMedicalNotes());
        existing.setMedicalPrescription(resident.getMedicalPrescription());
        existing.setRoomNumber(resident.getRoomNumber());
        existing.setOccupation(resident.getOccupation());
        existing.setDisability(resident.getDisability());
        existing.setAadhaarNumber(resident.getAadhaarNumber());

        // Only update photo if a new one is set
        if (resident.getPhoto() != null) {
            existing.setPhoto(resident.getPhoto());
        }

        Resident updated = residentRepository.save(existing);
        auditService.logActivity(com.oldagehome.portal.audit.AuditModule.RESIDENT,
                com.oldagehome.portal.audit.AuditAction.UPDATE, "Updated resident: " + updated.getFullName(),
                "Resident", updated.getId(), true, null);
        return updated;
    }

    @Override
    public void deleteResident(Long id) {
        Resident existing = getResidentById(id);
        residentRepository.delete(existing);
        auditService.logActivity(com.oldagehome.portal.audit.AuditModule.RESIDENT,
                com.oldagehome.portal.audit.AuditAction.DELETE, "Deleted resident: " + existing.getFullName(),
                "Resident", id, true, null);
    }

    @Override
    public void deleteAllResidents() {
        residentRepository.deleteAllInBatch();
        auditService.logActivity(com.oldagehome.portal.audit.AuditModule.RESIDENT,
                com.oldagehome.portal.audit.AuditAction.DELETE, "Deleted all residents",
                "Resident", null, true, null);
    }

    @Override
    public boolean isDuplicateResident(Resident resident) {
        List<Resident> potentials = residentRepository.findPotentialDuplicates(
                resident.getFullName(), resident.getDateOfBirth(), resident.getMobile(), resident.getBloodGroup());
        
        for (Resident p : potentials) {
            // If it's the same resident being updated, skip
            if (resident.getId() != null && resident.getId().equals(p.getId())) {
                continue;
            }
            
            boolean guardianMatch = (resident.getGuardianName() == null && p.getGuardianName() == null) ||
                                    (resident.getGuardianName() != null && resident.getGuardianName().equals(p.getGuardianName()));
            boolean addressMatch = (resident.getAddress() == null && p.getAddress() == null) ||
                                   (resident.getAddress() != null && resident.getAddress().equals(p.getAddress()));
            
            if (guardianMatch && addressMatch) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<ResidentImportDTO> importFromExcel(MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Excel file is empty");
        }

        List<ResidentImportDTO> dtos;
        try (InputStream is = file.getInputStream()) {
            dtos = ResidentExcelImporter.importResidents(is);
        }

        int successCount = 0;
        int failCount = 0;
        java.util.Set<String> processedSignatures = new java.util.HashSet<>();

        // Validate and insert into database
        for (ResidentImportDTO dto : dtos) {
            if (dto.isValid()) {
                try {
                    // Create Resident object and persist
                    Resident resident = Resident.builder()
                            .fullName(dto.getFullName())
                            .gender(dto.getGender())
                            .dateOfBirth(dto.getDateOfBirth())
                            .mobile(dto.getMobile())
                            .guardianName(dto.getGuardianName())
                            .guardianPhone(dto.getGuardianPhone())
                            .guardianEmail(dto.getGuardianEmail())
                            .guardianAddress(dto.getGuardianAddress())
                            .medicalPrescription(dto.getMedicalPrescription())
                            .roomNumber(dto.getRoomNumber())
                            .occupation(dto.getOccupation())
                            .disability(dto.getDisability())
                            .aadhaarNumber(dto.getAadhaarNumber())
                            .joiningDate(LocalDate.now()) // Default joining date as today
                            .status(ResidentStatus.ACTIVE)
                            .build();

                    // 1. Check for duplicates within the same file to prevent double-inserts
                    String signature = resident.getFullName() + "|" + resident.getDateOfBirth() + "|" + resident.getMobile();
                    if (processedSignatures.contains(signature)) {
                        dto.setValid(false);
                        dto.setErrorMessage("Skipped: Duplicate within the same file.");
                        failCount++;
                        continue;
                    }
                    processedSignatures.add(signature);

                    // 2. Check for duplicates against the database
                    if (isDuplicateResident(resident)) {
                        dto.setValid(false);
                        dto.setErrorMessage("Skipped: Already exists in the system.");
                        failCount++;
                        continue;
                    }

                    residentRepository.save(resident);
                    successCount++;
                } catch (Exception e) {
                    dto.setValid(false);
                    dto.setErrorMessage("Failed to save: " + e.getMessage());
                    failCount++;
                }
            } else {
                failCount++;
            }
        }

        auditService.logActivity(com.oldagehome.portal.audit.AuditModule.RESIDENT,
                com.oldagehome.portal.audit.AuditAction.IMPORT,
                "Imported residents from Excel. Success: " + successCount + ", Failed: " + failCount, "Resident", null,
                true, null);

        return dtos;
    }
}
