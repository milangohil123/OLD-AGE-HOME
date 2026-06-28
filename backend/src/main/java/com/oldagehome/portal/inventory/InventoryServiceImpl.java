package com.oldagehome.portal.inventory;

import com.oldagehome.portal.dto.InventoryImportDTO;
import com.oldagehome.portal.excel.InventoryExcelExporter;
import com.oldagehome.portal.excel.InventoryExcelImporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final com.oldagehome.portal.audit.AuditService auditService;

    @Autowired
    public InventoryServiceImpl(InventoryRepository inventoryRepository,
                                com.oldagehome.portal.audit.AuditService auditService) {
        this.inventoryRepository = inventoryRepository;
        this.auditService = auditService;
    }

    // -------------------------------------------------------------------------
    // Core CRUD
    // -------------------------------------------------------------------------

    @Override
    public Page<Inventory> getInventory(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return inventoryRepository.searchInventory(keyword.trim(), pageable);
        }
        return inventoryRepository.findAll(pageable);
    }

    @Override
    public List<Inventory> getAllInventory() {
        return inventoryRepository.findAll();
    }

    @Override
    public Inventory getInventoryById(Long id) {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> {
                    auditService.logActivity(com.oldagehome.portal.audit.AuditModule.INVENTORY, com.oldagehome.portal.audit.AuditAction.VIEW, "Failed to view medicine: not found", "Inventory", id, false, "Medicine not found");
                    return new RuntimeException("Inventory item not found with id: " + id);
                });
    }

    @Override
    public Inventory saveInventory(Inventory inventory) {
        // Business Rule 1: Medicine Code must be unique
        if (inventoryRepository.existsByMedicineCode(inventory.getMedicineCode())) {
            auditService.logActivity(com.oldagehome.portal.audit.AuditModule.INVENTORY, com.oldagehome.portal.audit.AuditAction.CREATE, "Failed to save medicine: Code already exists", "Inventory", null, false, "Duplicate Medicine Code: " + inventory.getMedicineCode());
            throw new RuntimeException("Medicine Code '" + inventory.getMedicineCode() + "' already exists in the system.");
        }

        // Business Rule 2: Quantity cannot be negative
        if (inventory.getQuantity() != null && inventory.getQuantity() < 0) {
            auditService.logActivity(com.oldagehome.portal.audit.AuditModule.INVENTORY, com.oldagehome.portal.audit.AuditAction.CREATE, "Failed to save medicine: Negative quantity", "Inventory", null, false, "Negative quantity not allowed");
            throw new RuntimeException("Quantity cannot be negative.");
        }

        // Business Rule 3 & 4: Purchase Date cannot be after Expiry Date
        validateDates(inventory);

        Inventory saved = inventoryRepository.save(inventory);
        auditService.logActivity(com.oldagehome.portal.audit.AuditModule.INVENTORY, com.oldagehome.portal.audit.AuditAction.CREATE, "Created medicine record: " + saved.getMedicineName(), "Inventory", saved.getId(), true, null);
        return saved;
    }

    @Override
    public Inventory updateInventory(Inventory inventory) {
        Inventory existing = getInventoryById(inventory.getId());

        // If medicine code changed, verify uniqueness
        if (!existing.getMedicineCode().equals(inventory.getMedicineCode())) {
            if (inventoryRepository.existsByMedicineCode(inventory.getMedicineCode())) {
                auditService.logActivity(com.oldagehome.portal.audit.AuditModule.INVENTORY, com.oldagehome.portal.audit.AuditAction.UPDATE, "Failed to update medicine: Code already exists", "Inventory", inventory.getId(), false, "Duplicate Medicine Code: " + inventory.getMedicineCode());
                throw new RuntimeException("Medicine Code '" + inventory.getMedicineCode() + "' already exists in the system.");
            }
        }

        // Validate quantity
        if (inventory.getQuantity() != null && inventory.getQuantity() < 0) {
            auditService.logActivity(com.oldagehome.portal.audit.AuditModule.INVENTORY, com.oldagehome.portal.audit.AuditAction.UPDATE, "Failed to update medicine: Negative quantity", "Inventory", inventory.getId(), false, "Negative quantity not allowed");
            throw new RuntimeException("Quantity cannot be negative.");
        }

        // Validate dates
        validateDates(inventory);

        // Merge all editable fields
        existing.setMedicineCode(inventory.getMedicineCode());
        existing.setMedicineName(inventory.getMedicineName());
        existing.setCategory(inventory.getCategory());
        existing.setManufacturer(inventory.getManufacturer());
        existing.setSupplier(inventory.getSupplier());
        existing.setBatchNumber(inventory.getBatchNumber());
        existing.setPurchaseDate(inventory.getPurchaseDate());
        existing.setExpiryDate(inventory.getExpiryDate());
        existing.setQuantity(inventory.getQuantity());
        existing.setMinimumStock(inventory.getMinimumStock());
        existing.setUnitPrice(inventory.getUnitPrice());
        existing.setRackLocation(inventory.getRackLocation());
        existing.setNotes(inventory.getNotes());

        Inventory updated = inventoryRepository.save(existing);
        auditService.logActivity(com.oldagehome.portal.audit.AuditModule.INVENTORY, com.oldagehome.portal.audit.AuditAction.UPDATE, "Updated medicine record: " + updated.getMedicineName(), "Inventory", updated.getId(), true, null);
        return updated;
    }

    @Override
    public void deleteInventory(Long id) {
        Inventory existing = getInventoryById(id);
        inventoryRepository.delete(existing);
        auditService.logActivity(com.oldagehome.portal.audit.AuditModule.INVENTORY, com.oldagehome.portal.audit.AuditAction.DELETE, "Deleted medicine record: " + existing.getMedicineName(), "Inventory", id, true, null);
    }

    @Override
    public boolean existsByMedicineCode(String medicineCode) {
        return inventoryRepository.existsByMedicineCode(medicineCode);
    }

    // -------------------------------------------------------------------------
    // Excel Import
    // -------------------------------------------------------------------------

    @Override
    public List<InventoryImportDTO> importFromExcel(MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Excel file is empty.");
        }

        List<InventoryImportDTO> dtos;
        try (InputStream is = file.getInputStream()) {
            dtos = InventoryExcelImporter.importInventory(is);
        }

        int successCount = 0;
        int failCount = 0;

        for (InventoryImportDTO dto : dtos) {
            if (!dto.isValid()) {
                failCount++;
                continue;
            }

            // Duplicate check
            if (inventoryRepository.existsByMedicineCode(dto.getMedicineCode())) {
                dto.setValid(false);
                dto.setErrorMessage("Medicine Code already exists in system.");
                failCount++;
                continue;
            }

            try {
                Inventory item = Inventory.builder()
                        .medicineCode(dto.getMedicineCode())
                        .medicineName(dto.getMedicineName())
                        .category(dto.getCategory())
                        .manufacturer(dto.getManufacturer())
                        .supplier(dto.getSupplier())
                        .batchNumber(dto.getBatchNumber())
                        .purchaseDate(dto.getPurchaseDate() != null ? dto.getPurchaseDate() : LocalDate.now())
                        .expiryDate(dto.getExpiryDate())
                        .quantity(dto.getQuantity() != null ? dto.getQuantity() : 0)
                        .minimumStock(dto.getMinimumStock() != null ? dto.getMinimumStock() : 10)
                        .unitPrice(dto.getUnitPrice())
                        .rackLocation(dto.getRackLocation())
                        .notes(dto.getNotes())
                        .status(InventoryStatus.AVAILABLE)
                        .build();

                inventoryRepository.save(item);
                successCount++;
            } catch (Exception e) {
                dto.setValid(false);
                dto.setErrorMessage("Failed to save: " + e.getMessage());
                failCount++;
            }
        }

        auditService.logActivity(com.oldagehome.portal.audit.AuditModule.INVENTORY, com.oldagehome.portal.audit.AuditAction.IMPORT, 
            "Imported medicines from Excel. Success: " + successCount + ", Failed: " + failCount, "Inventory", null, true, null);

        return dtos;
    }

    // -------------------------------------------------------------------------
    // Excel Export
    // -------------------------------------------------------------------------

    @Override
    public byte[] exportToExcel() throws IOException {
        List<Inventory> items = getAllInventory();
        return InventoryExcelExporter.exportInventory(items);
    }

    // -------------------------------------------------------------------------
    // Dashboard Statistics
    // -------------------------------------------------------------------------

    @Override
    public long countTotalMedicines() {
        return inventoryRepository.count();
    }

    @Override
    public long countAvailable() {
        return inventoryRepository.countByStatus(InventoryStatus.AVAILABLE);
    }

    @Override
    public long countLowStock() {
        return inventoryRepository.countByStatus(InventoryStatus.LOW_STOCK);
    }

    @Override
    public long countOutOfStock() {
        return inventoryRepository.countByStatus(InventoryStatus.OUT_OF_STOCK);
    }

    @Override
    public long countExpired() {
        return inventoryRepository.countByStatus(InventoryStatus.EXPIRED);
    }

    @Override
    public long countExpiringSoon() {
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(30); // medicines expiring within 30 days
        return inventoryRepository.countExpiringSoon(today, threshold);
    }

    @Override
    public long countTodayPurchases() {
        return inventoryRepository.countByPurchaseDate(LocalDate.now());
    }

    // -------------------------------------------------------------------------
    // Private Helpers
    // -------------------------------------------------------------------------

    private void validateDates(Inventory inventory) {
        if (inventory.getPurchaseDate() != null && inventory.getExpiryDate() != null) {
            if (inventory.getPurchaseDate().isAfter(inventory.getExpiryDate())) {
                throw new RuntimeException("Purchase Date cannot be after Expiry Date.");
            }
        }
    }
}
