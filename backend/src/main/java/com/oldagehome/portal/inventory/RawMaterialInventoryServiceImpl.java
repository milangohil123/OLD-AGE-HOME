package com.oldagehome.portal.inventory;

import com.oldagehome.portal.utils.QuantityParserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RawMaterialInventoryServiceImpl implements RawMaterialInventoryService {

    private final RawMaterialInventoryRepository rawMaterialInventoryRepository;
    private final InventoryUsageRepository inventoryUsageRepository;
    private final com.oldagehome.portal.audit.AuditService auditService;

    @Autowired
    public RawMaterialInventoryServiceImpl(RawMaterialInventoryRepository rawMaterialInventoryRepository,
                                           InventoryUsageRepository inventoryUsageRepository,
                                           com.oldagehome.portal.audit.AuditService auditService) {
        this.rawMaterialInventoryRepository = rawMaterialInventoryRepository;
        this.inventoryUsageRepository = inventoryUsageRepository;
        this.auditService = auditService;
    }

    @Override
    public Page<RawMaterialInventory> searchInventory(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return rawMaterialInventoryRepository.searchRawMaterials(keyword.trim(), pageable);
        }
        return rawMaterialInventoryRepository.findAll(pageable);
    }

    @Override
    public RawMaterialInventory addStock(String itemName, String rawQuantity) {
        if (itemName == null || itemName.trim().isEmpty()) {
            return null;
        }

        String normalizedName = itemName.trim().toLowerCase();
        QuantityParserUtil.ParsedQuantity parsed = QuantityParserUtil.parseQuantity(rawQuantity);

        if (parsed.amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null; // Don't add zero or negative
        }

        Optional<RawMaterialInventory> existingOpt = rawMaterialInventoryRepository.findByItemNameIgnoreCase(normalizedName);
        RawMaterialInventory inventory;

        if (existingOpt.isPresent()) {
            inventory = existingOpt.get();
            // Assuming unit is same or compatible
            inventory.setTotalQuantity(inventory.getTotalQuantity().add(parsed.amount));
        } else {
            inventory = RawMaterialInventory.builder()
                    .itemName(itemName.trim())
                    .totalQuantity(parsed.amount)
                    .unit(parsed.unit)
                    .build();
        }

        RawMaterialInventory saved = rawMaterialInventoryRepository.save(inventory);
        auditService.logActivity(com.oldagehome.portal.audit.AuditModule.INVENTORY,
                com.oldagehome.portal.audit.AuditAction.CREATE,
                "Added stock for raw material: " + saved.getItemName() + " (" + parsed.amount + " " + parsed.unit + ")",
                "RawMaterialInventory", saved.getId(), true, null);
        return saved;
    }

    @Override
    public void deductStock(String itemName, BigDecimal amountUsed, String unit) {
        if (itemName == null || itemName.trim().isEmpty()) {
            return;
        }

        Optional<RawMaterialInventory> existingOpt = rawMaterialInventoryRepository.findByItemNameIgnoreCase(itemName.trim());
        if (existingOpt.isPresent()) {
            RawMaterialInventory inventory = existingOpt.get();
            
            // Normalize amount based on unit if necessary. For now, we assume standard unit is used.
            // A more complex system would convert `amountUsed` `unit` to `inventory.getUnit()`.
            QuantityParserUtil.ParsedQuantity parsed = QuantityParserUtil.parseQuantity(amountUsed.toString() + " " + unit);
            
            BigDecimal newQuantity = inventory.getTotalQuantity().subtract(parsed.amount);
            if (newQuantity.compareTo(BigDecimal.ZERO) < 0) {
                newQuantity = BigDecimal.ZERO; // Prevent negative stock
            }
            inventory.setTotalQuantity(newQuantity);
            rawMaterialInventoryRepository.save(inventory);
        }
    }

    @Override
    public InventoryUsage logUsage(LocalDate usageDate, String purpose, List<RawMaterialUsageFormDTO.UsageItemDTO> items) {
        InventoryUsage usage = InventoryUsage.builder()
                .usageDate(usageDate)
                .purpose(purpose)
                .build();

        for (RawMaterialUsageFormDTO.UsageItemDTO dtoItem : items) {
            String itemName = dtoItem.getRawMaterialName().trim();
            
            // Check if RawMaterialInventory exists, if not create it
            Optional<RawMaterialInventory> existingOpt = rawMaterialInventoryRepository.findByItemNameIgnoreCase(itemName);
            RawMaterialInventory rawMaterial;
            if (existingOpt.isPresent()) {
                rawMaterial = existingOpt.get();
            } else {
                rawMaterial = RawMaterialInventory.builder()
                        .itemName(itemName)
                        .totalQuantity(BigDecimal.ZERO)
                        .unit(dtoItem.getUnit())
                        .build();
                rawMaterial = rawMaterialInventoryRepository.save(rawMaterial);
            }

            InventoryUsageItem usageItem = InventoryUsageItem.builder()
                    .rawMaterial(rawMaterial)
                    .quantityUsed(dtoItem.getQuantityUsed())
                    .unit(dtoItem.getUnit())
                    .inventoryUsage(usage)
                    .build();

            usage.getItems().add(usageItem);
            deductStock(itemName, usageItem.getQuantityUsed(), usageItem.getUnit());
        }

        InventoryUsage saved = inventoryUsageRepository.save(usage);
        auditService.logActivity(com.oldagehome.portal.audit.AuditModule.INVENTORY,
                com.oldagehome.portal.audit.AuditAction.CREATE,
                "Logged usage of raw materials for: " + purpose,
                "InventoryUsage", saved.getId(), true, null);
        return saved;
    }

    @Override
    public Page<InventoryUsage> searchUsages(String purpose, LocalDate date, Pageable pageable) {
        return inventoryUsageRepository.searchUsages(purpose, date, pageable);
    }

    @Override
    public long countTotalRawMaterials() {
        return rawMaterialInventoryRepository.count();
    }

    @Override
    public long countLowStockMaterials(BigDecimal threshold) {
        return rawMaterialInventoryRepository.countLowStock(threshold);
    }

    @Override
    public long countUsagesToday() {
        return inventoryUsageRepository.countUsagesToday(LocalDate.now());
    }
}
