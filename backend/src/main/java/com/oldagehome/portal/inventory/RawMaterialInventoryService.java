package com.oldagehome.portal.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RawMaterialInventoryService {

    Page<RawMaterialInventory> searchInventory(String keyword, Pageable pageable);
    
    RawMaterialInventory addStock(String itemName, String rawQuantity);
    
    void deductStock(String itemName, BigDecimal amountUsed, String unit);
    
    InventoryUsage logUsage(LocalDate usageDate, String purpose, java.util.List<InventoryUsageItem> items);
    
    Page<InventoryUsage> searchUsages(String purpose, LocalDate date, Pageable pageable);
    
    long countTotalRawMaterials();
    
    long countLowStockMaterials(BigDecimal threshold);
    
    long countUsagesToday();
}
