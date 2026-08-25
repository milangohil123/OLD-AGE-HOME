package com.oldagehome.portal.inventory;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_usage_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryUsageItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_usage_id", nullable = false)
    private InventoryUsage inventoryUsage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_material_id", nullable = false)
    private RawMaterialInventory rawMaterial;

    @NotNull(message = "Quantity Used is required")
    @DecimalMin(value = "0.001", message = "Quantity used must be greater than 0")
    @Column(name = "quantity_used", nullable = false, precision = 10, scale = 3)
    private BigDecimal quantityUsed;

    @NotBlank(message = "Unit is required")
    @Column(nullable = false, length = 20)
    private String unit; // Record the unit used at the time of entry

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
