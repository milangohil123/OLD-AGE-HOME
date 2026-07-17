package com.oldagehome.portal.donor;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a single medicine item donated within a MEDICINE-type donor record.
 * Stored in the {@code medicine_donation_items} table (already created in Supabase).
 */
@Entity
@Table(name = "medicine_donation_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicineDonationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donor_id", nullable = false)
    private Donor donor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donation_id")
    private Donation donation;

    @Column(name = "medicine_name", nullable = false, length = 200)
    private String medicineName;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "display_order")
    private Integer displayOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
