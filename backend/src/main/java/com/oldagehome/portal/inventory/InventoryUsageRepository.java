package com.oldagehome.portal.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface InventoryUsageRepository extends JpaRepository<InventoryUsage, Long> {
    
    @Query("SELECT u FROM InventoryUsage u WHERE " +
           "(:purpose IS NULL OR LOWER(u.purpose) LIKE LOWER(CONCAT('%', :purpose, '%'))) AND " +
           "(cast(:date as date) IS NULL OR u.usageDate = :date)")
    Page<InventoryUsage> searchUsages(@Param("purpose") String purpose, @Param("date") LocalDate date, Pageable pageable);
    
    @Query("SELECT COUNT(u) FROM InventoryUsage u WHERE u.usageDate = :today")
    long countUsagesToday(@Param("today") LocalDate today);

    @Query("SELECT SUM(i.quantityUsed) FROM InventoryUsageItem i WHERE LOWER(i.rawMaterial.itemName) = LOWER(:itemName)")
    Double sumUsedQuantityByItemName(@Param("itemName") String itemName);
}
