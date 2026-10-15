package com.oldagehome.portal.donor;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a single food item donated within a FOOD-type donor record.
 * Stored in the {@code food_donation_items} table (already created in Supabase).
 */
@Entity
@Table(name = "food_donation_items")
@Data
public class FoodDonationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donor_id", nullable = false)
    private Donor donor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donation_id")
    private Donation donation;

    @Column(name = "food_name", nullable = false, length = 200)
    private String foodName;

    @Column(length = 100)
    private String quantity;

    @Column(name = "display_order")
    private Integer displayOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public FoodDonationItem() {
    }

    @Builder
    public FoodDonationItem(Long id, Donor donor, Donation donation, String foodName, String quantity,
                            Integer displayOrder, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.donor = donor;
        this.donation = donation;
        this.foodName = foodName;
        this.quantity = quantity;
        this.displayOrder = displayOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
