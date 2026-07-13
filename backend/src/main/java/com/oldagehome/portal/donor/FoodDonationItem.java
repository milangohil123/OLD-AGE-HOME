package com.oldagehome.portal.donor;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodDonationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donor_id", nullable = false)
    private Donor donor;

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
}
