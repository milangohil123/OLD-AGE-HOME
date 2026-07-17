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
import java.util.*;

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
    public DonorFormDTO getDonorFormDtoById(Long id) {
        Donor donor = getDonorById(id);
        
        DonorFormDTO dto = DonorFormDTO.builder()
                .id(donor.getId())
                .fullName(donor.getFullName())
                .gender(donor.getGender())
                .dateOfBirth(donor.getDateOfBirth())
                .mobile(donor.getMobile())
                .email(donor.getEmail())
                .address(donor.getAddress())
                .city(donor.getCity())
                .state(donor.getState())
                .pincode(donor.getPincode())
                .donationFrequency(donor.getDonationFrequency())
                .donationType(donor.getDonationType())
                .donationAmount(donor.getDonationAmount())
                .donationDate(donor.getDonationDate())
                .paymentMethod(donor.getPaymentMethod())
                .transactionId(donor.getTransactionId())
                .remarks(donor.getRemarks())
                .status(donor.getStatus())
                .photo(donor.getPhoto())
                .build();

        if (donor.getDonationType() == DonationType.MEDICINE && donor.getMedicineItems() != null) {
            List<DonorFormDTO.MedicineItemDTO> medDtos = new ArrayList<>();
            for (MedicineDonationItem item : donor.getMedicineItems()) {
                medDtos.add(new DonorFormDTO.MedicineItemDTO(item.getId(), item.getMedicineName(), item.getPrice(), item.getExpiryDate()));
            }
            dto.setMedicineItems(medDtos);
        }

        if (donor.getDonationType() == DonationType.FOOD && donor.getFoodItems() != null) {
            List<DonorFormDTO.FoodItemDTO> foodDtos = new ArrayList<>();
            for (FoodDonationItem item : donor.getFoodItems()) {
                foodDtos.add(new DonorFormDTO.FoodItemDTO(item.getId(), item.getFoodName(), item.getQuantity()));
            }
            dto.setFoodItems(foodDtos);
        }

        return dto;
    }

    @Override
    public Donor saveDonor(DonorFormDTO dto) {
        Donor donor = new Donor();
        mapDtoToEntity(dto, donor);
        
        Donor saved = donorRepository.save(donor);
        
        auditService.logActivity(com.oldagehome.portal.audit.AuditModule.DONOR, com.oldagehome.portal.audit.AuditAction.CREATE, "Created donor record for: " + saved.getFullName(), "Donor", saved.getId(), true, null);
        
        BigDecimal amount = saved.getDonationAmount() != null ? saved.getDonationAmount() : BigDecimal.ZERO;
        auditService.logActivity(com.oldagehome.portal.audit.AuditModule.DONATION, com.oldagehome.portal.audit.AuditAction.CREATE, "Created donation record for: " + saved.getFullName() + ", Type: " + saved.getDonationType() + ", Amount: ₹" + amount, "Donation", saved.getId(), true, null);
        
        return saved;
    }

    @Override
    public Donor updateDonor(DonorFormDTO dto) {
        Donor existing = getDonorById(dto.getId());
        mapDtoToEntity(dto, existing);
        
        Donor updated = donorRepository.save(existing);
        
        auditService.logActivity(com.oldagehome.portal.audit.AuditModule.DONOR, com.oldagehome.portal.audit.AuditAction.UPDATE, "Updated donor record for: " + updated.getFullName(), "Donor", updated.getId(), true, null);
        
        return updated;
    }

    @Override
    public Donor saveDonor(Donor donor) {
        Donor saved = donorRepository.save(donor);
        auditService.logActivity(com.oldagehome.portal.audit.AuditModule.DONOR, com.oldagehome.portal.audit.AuditAction.CREATE, "Created donor record for: " + saved.getFullName(), "Donor", saved.getId(), true, null);
        return saved;
    }

    @Override
    public void deleteDonor(Long id) {
        Donor existing = getDonorById(id);
        donorRepository.delete(existing);
        auditService.logActivity(com.oldagehome.portal.audit.AuditModule.DONOR, com.oldagehome.portal.audit.AuditAction.DELETE, "Deleted donor record of: " + existing.getFullName(), "Donor", id, true, null);
    }

    private void mapDtoToEntity(DonorFormDTO dto, Donor donor) {
        donor.setFullName(dto.getFullName());
        donor.setGender(dto.getGender());
        donor.setDateOfBirth(dto.getDateOfBirth());
        donor.setMobile(dto.getMobile());
        donor.setEmail(dto.getEmail());
        donor.setAddress(dto.getAddress());
        donor.setCity(dto.getCity());
        donor.setState(dto.getState());
        donor.setPincode(dto.getPincode());
        donor.setDonationFrequency(dto.getDonationFrequency());
        donor.setDonationType(dto.getDonationType());
        
        // Handle specific type requirements
        if (dto.getDonationType() == DonationType.MEDICINE || dto.getDonationType() == DonationType.FOOD) {
            donor.setDonationAmount(BigDecimal.ZERO);
        } else {
            donor.setDonationAmount(dto.getDonationAmount());
        }
        
        donor.setDonationDate(dto.getDonationDate());
        donor.setPaymentMethod(dto.getPaymentMethod());
        donor.setTransactionId(dto.getTransactionId());
        donor.setRemarks(dto.getRemarks());
        donor.setStatus(dto.getStatus());
        
        if (dto.getPhoto() != null && !dto.getPhoto().isBlank()) {
            donor.setPhoto(dto.getPhoto());
        }

        // Handle nested lists with clear & re-add to manage Hibernate orphans
        if (dto.getDonationType() == DonationType.MEDICINE) {
            donor.getFoodItems().clear();
            donor.getMedicineItems().clear();
            if (dto.getMedicineItems() != null) {
                int displayOrder = 1;
                for (DonorFormDTO.MedicineItemDTO itemDto : dto.getMedicineItems()) {
                    if (itemDto.getMedicineName() == null || itemDto.getMedicineName().isBlank()) continue;
                    MedicineDonationItem item = MedicineDonationItem.builder()
                            .donor(donor)
                            .medicineName(itemDto.getMedicineName().trim())
                            .price(itemDto.getPrice() != null ? itemDto.getPrice() : BigDecimal.ZERO)
                            .expiryDate(itemDto.getExpiryDate())
                            .displayOrder(displayOrder++)
                            .build();
                    donor.getMedicineItems().add(item);
                }
            }
        } else if (dto.getDonationType() == DonationType.FOOD) {
            donor.getMedicineItems().clear();
            donor.getFoodItems().clear();
            if (dto.getFoodItems() != null) {
                int displayOrder = 1;
                for (DonorFormDTO.FoodItemDTO itemDto : dto.getFoodItems()) {
                    if (itemDto.getFoodName() == null || itemDto.getFoodName().isBlank()) continue;
                    FoodDonationItem item = FoodDonationItem.builder()
                            .donor(donor)
                            .foodName(itemDto.getFoodName().trim())
                            .quantity(itemDto.getQuantity() != null ? itemDto.getQuantity().trim() : "")
                            .displayOrder(displayOrder++)
                            .build();
                    donor.getFoodItems().add(item);
                }
            }
        } else {
            donor.getMedicineItems().clear();
            donor.getFoodItems().clear();
        }
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

        // Group rows so that multiple medicine or food lines are aggregated under one Donor
        Map<String, Donor> activeImportMap = new HashMap<>();

        for (DonorImportDTO dto : dtos) {
            if (!dto.isValid()) {
                failCount++;
                continue;
            }

            try {
                // Key to identify a unique donor/donation event
                String key = dto.getFullName() + "|" +
                             (dto.getMobile() != null ? dto.getMobile() : "") + "|" +
                             (dto.getEmail() != null ? dto.getEmail() : "") + "|" +
                             dto.getDonationDate().toString() + "|" +
                             (dto.getDonationType() != null ? dto.getDonationType().name() : "") + "|" +
                             (dto.getTransactionId() != null ? dto.getTransactionId() : "");

                Donor donor = activeImportMap.get(key);
                boolean isNew = false;

                if (donor == null) {
                    donor = Donor.builder()
                            .fullName(dto.getFullName())
                            .gender(dto.getGender() != null ? dto.getGender() : "OTHER")
                            .dateOfBirth(LocalDate.of(1980, 1, 1)) // Default DOB for imported records
                            .mobile(dto.getMobile())
                            .email(dto.getEmail())
                            .address(dto.getAddress())
                            .donationFrequency(dto.getDonationFrequency() != null ? dto.getDonationFrequency() : DonationFrequency.ONE_TIME)
                            .donationType(dto.getDonationType())
                            .donationAmount(dto.getDonationAmount() != null ? dto.getDonationAmount() : BigDecimal.ZERO)
                            .donationDate(dto.getDonationDate())
                            .paymentMethod(dto.getPaymentMethod())
                            .transactionId(dto.getTransactionId())
                            .status(dto.getStatus() != null ? dto.getStatus() : DonorStatus.ACTIVE)
                            .medicineItems(new ArrayList<>())
                            .foodItems(new ArrayList<>())
                            .build();
                    isNew = true;
                }

                // Add item details if applicable
                if (dto.getDonationType() == DonationType.MEDICINE && dto.getMedicineName() != null && !dto.getMedicineName().isEmpty()) {
                    int nextOrder = donor.getMedicineItems().size() + 1;
                    MedicineDonationItem item = MedicineDonationItem.builder()
                            .donor(donor)
                            .medicineName(dto.getMedicineName())
                            .price(dto.getMedicinePrice() != null ? dto.getMedicinePrice() : BigDecimal.ZERO)
                            .expiryDate(dto.getMedicineExpiryDate())
                            .displayOrder(nextOrder)
                            .build();
                    donor.getMedicineItems().add(item);
                } else if (dto.getDonationType() == DonationType.FOOD && dto.getFoodName() != null && !dto.getFoodName().isEmpty()) {
                    int nextOrder = donor.getFoodItems().size() + 1;
                    FoodDonationItem item = FoodDonationItem.builder()
                            .donor(donor)
                            .foodName(dto.getFoodName())
                            .quantity(dto.getFoodQuantity())
                            .displayOrder(nextOrder)
                            .build();
                    donor.getFoodItems().add(item);
                }

                // Save or update repository
                donorRepository.save(donor);
                if (isNew) {
                    activeImportMap.put(key, donor);
                }
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
