package com.oldagehome.portal.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RawMaterialInventoryRepository extends JpaRepository<RawMaterialInventory, Long> {
    
    Optional<RawMaterialInventory> findByItemNameIgnoreCase(String itemName);
    
    @Query("SELECT r FROM RawMaterialInventory r WHERE LOWER(r.itemName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<RawMaterialInventory> searchRawMaterials(@Param("keyword") String keyword, Pageable pageable);
    
    long count();
    
    @Query("SELECT COUNT(r) FROM RawMaterialInventory r WHERE r.totalQuantity <= :threshold")
    long countLowStock(@Param("threshold") java.math.BigDecimal threshold);
}
