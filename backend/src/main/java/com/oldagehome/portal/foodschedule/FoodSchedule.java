package com.oldagehome.portal.foodschedule;

import com.oldagehome.portal.donor.Donor;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Represents a single meal entry in the food schedule.
 * Maps to the food_schedule table in Supabase PostgreSQL.
 */
@Entity
@Table(name = "food_schedule")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Schedule date is required")
    @Column(name = "schedule_date", nullable = false)
    private LocalDate scheduleDate;

    @NotNull(message = "Meal type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false, length = 20)
    private MealType mealType;

    @NotNull(message = "Serving time is required")
    @Column(name = "serving_time", nullable = false)
    private LocalTime servingTime;

    @NotBlank(message = "Menu items are required")
    @Column(name = "menu_items", nullable = false, columnDefinition = "TEXT")
    private String menuItems;

    /**
     * Optional link to the existing Donor record.
     * Nullable — if donor is not found, manualDonorName is used instead.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donor_id", nullable = true)
    private Donor donor;

    /**
     * Fallback donor name when donor is not in the system.
     */
    @Column(name = "manual_donor_name", length = 255)
    private String manualDonorName;

    @NotNull(message = "Sponsorship type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "sponsorship_type", nullable = false, length = 30)
    private SponsorshipType sponsorshipType;

    @NotNull(message = "Amount is required")
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Convenience method: returns display name of the donor for the UI.
     * Uses manualDonorName as fallback if donor entity is not linked.
     */
    public String getDonorDisplayName() {
        if (donor != null) {
            return donor.getFullName();
        }
        return manualDonorName != null ? manualDonorName : "—";
    }
}
