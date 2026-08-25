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
import java.time.LocalDateTime;

@Entity
@Table(name = "raw_material_inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RawMaterialInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Item Name is required")
    @Column(name = "item_name", unique = true, nullable = false, length = 150)
    private String itemName;

    @NotNull(message = "Total Quantity is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Total Quantity cannot be negative")
    @Column(name = "total_quantity", nullable = false, precision = 10, scale = 3)
    private BigDecimal totalQuantity;

    @NotBlank(message = "Unit is required")
    @Column(nullable = false, length = 20)
    private String unit; // e.g., "KG", "LITER", "UNIT"

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
