package com.oldagehome.portal.inventory;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inventory_usage")
@Data
public class InventoryUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Usage Date is required")
    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @NotBlank(message = "Purpose is required")
    @Column(nullable = false, length = 150)
    private String purpose; // e.g., "Breakfast", "Lunch", "General Use"

    @OneToMany(mappedBy = "inventoryUsage", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InventoryUsageItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public InventoryUsage() {
    }

    @Builder
    public InventoryUsage(Long id, LocalDate usageDate, String purpose, List<InventoryUsageItem> items,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.usageDate = usageDate;
        this.purpose = purpose;
        this.items = items != null ? items : new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
