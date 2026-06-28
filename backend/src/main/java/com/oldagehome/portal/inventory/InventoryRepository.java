package com.oldagehome.portal.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByMedicineCode(String medicineCode);

    boolean existsByMedicineCode(String medicineCode);

    /**
     * Search medicines by code, name, manufacturer, supplier, or batch number.
     * Case-insensitive substring match.
     */
    @Query("SELECT i FROM Inventory i WHERE " +
           "LOWER(i.medicineCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(i.medicineName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(i.manufacturer) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(i.supplier) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(i.batchNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Inventory> searchInventory(@Param("keyword") String keyword, Pageable pageable);

    // --- Dashboard statistics queries ---

    /** Count all inventory items. */
    long count();

    /** Count by status. */
    long countByStatus(InventoryStatus status);

    /** Count medicines expiring within the next N days (not yet expired). */
    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.expiryDate > :today AND i.expiryDate <= :threshold")
    long countExpiringSoon(@Param("today") LocalDate today, @Param("threshold") LocalDate threshold);

    /** Count medicines purchased today. */
    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.purchaseDate = :today")
    long countByPurchaseDate(@Param("today") LocalDate today);
}
