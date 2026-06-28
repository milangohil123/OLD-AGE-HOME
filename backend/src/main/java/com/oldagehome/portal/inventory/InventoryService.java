package com.oldagehome.portal.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface InventoryService {

    /** Paginated list with optional keyword search. */
    Page<Inventory> getInventory(String keyword, Pageable pageable);

    /** Flat list (for export). */
    List<Inventory> getAllInventory();

    /** Fetch single inventory item by primary key. */
    Inventory getInventoryById(Long id);

    /** Create a new inventory record. */
    Inventory saveInventory(Inventory inventory);

    /** Update an existing inventory record. */
    Inventory updateInventory(Inventory inventory);

    /** Delete an inventory item by primary key. */
    void deleteInventory(Long id);

    /** Check uniqueness of medicineCode. */
    boolean existsByMedicineCode(String medicineCode);

    /** Parse, validate, persist from Excel; returns row-level result DTOs. */
    List<com.oldagehome.portal.dto.InventoryImportDTO> importFromExcel(MultipartFile file) throws Exception;

    /** Export all inventory records to an Excel byte array. */
    byte[] exportToExcel() throws IOException;

    // --- Dashboard stats ---
    long countTotalMedicines();
    long countAvailable();
    long countLowStock();
    long countOutOfStock();
    long countExpired();
    long countExpiringSoon();
    long countTodayPurchases();
}
