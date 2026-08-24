package com.oldagehome.portal.inventory;

import java.util.List;

public interface InventoryService {
    /**
     * Get dynamically aggregated inventory items from Donor donation records.
     * @param keyword Optional search keyword (matches item name or category)
     * @param category Optional category filter (e.g. "Food", "Medicine", "All")
     */
    List<InventoryItemDTO> getAggregatedInventory(String keyword, String category);
    
    // KPI Counters
    long countTotalItems();
    long countTotalQuantity(); // Optional: Might not make sense if units differ, but we can do a rough sum or count.
    long countFoodCategories();
    long countRecentContributions();
    
    // Legacy ReportController Compatibility Methods
    long countTotalMedicines();
    long countLowStock();
    long countExpired();
    long countAvailable();
}
