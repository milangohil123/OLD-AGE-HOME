package com.oldagehome.portal.inventory;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Medicine Code is required")
    @Column(name = "medicine_code", unique = true, nullable = false, length = 50)
    private String medicineCode;

    @NotBlank(message = "Medicine Name is required")
    @Size(min = 2, max = 150, message = "Medicine Name must be between 2 and 150 characters")
    @Column(name = "medicine_name", nullable = false, length = 150)
    private String medicineName;

    @NotNull(message = "Category is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MedicineCategory category;

    @Size(max = 100, message = "Manufacturer name must be at most 100 characters")
    @Column(length = 100)
    private String manufacturer;

    @Size(max = 100, message = "Supplier name must be at most 100 characters")
    @Column(length = 100)
    private String supplier;

    @Size(max = 50, message = "Batch Number must be at most 50 characters")
    @Column(name = "batch_number", length = 50)
    private String batchNumber;

    @NotNull(message = "Purchase Date is required")
    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @NotNull(message = "Expiry Date is required")
    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    @Column(nullable = false)
    private Integer quantity;

    @NotNull(message = "Minimum Stock is required")
    @Min(value = 0, message = "Minimum Stock cannot be negative")
    @Column(name = "minimum_stock", nullable = false)
    @Builder.Default
    private Integer minimumStock = 10;

    @NotNull(message = "Unit Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Unit Price cannot be negative")
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Size(max = 50, message = "Rack Location must be at most 50 characters")
    @Column(name = "rack_location", length = 50)
    private String rackLocation;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InventoryStatus status = InventoryStatus.AVAILABLE;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Automatically computes the correct status based on business rules
     * before persisting or updating the record.
     *
     * Rule priority:
     *   1. Expired      → today > expiryDate
     *   2. Out of Stock → quantity == 0
     *   3. Low Stock    → quantity <= minimumStock
     *   4. Available    → otherwise
     */
    @PrePersist
    @PreUpdate
    public void computeStatus() {
        if (expiryDate != null && LocalDate.now().isAfter(expiryDate)) {
            this.status = InventoryStatus.EXPIRED;
        } else if (quantity != null && quantity == 0) {
            this.status = InventoryStatus.OUT_OF_STOCK;
        } else if (quantity != null && minimumStock != null && quantity <= minimumStock) {
            this.status = InventoryStatus.LOW_STOCK;
        } else {
            this.status = InventoryStatus.AVAILABLE;
        }
    }
}
