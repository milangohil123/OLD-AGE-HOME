package com.oldagehome.portal.foodschedule;

import com.oldagehome.portal.donor.Donor;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_schedule")
@Data
public class FoodSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meal_date", nullable = false)
    private LocalDate mealDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", length = 20)
    private MealType mealType;

    @Column(name = "menu_items", columnDefinition = "TEXT")
    private String menuItems;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donor_id")
    private Donor donor;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(length = 20)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public FoodSchedule() {
    }

    @Builder
    public FoodSchedule(Long id, LocalDate mealDate, MealType mealType, String menuItems,
                        Donor donor, String remarks, String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.mealDate = mealDate;
        this.mealType = mealType;
        this.menuItems = menuItems;
        this.donor = donor;
        this.remarks = remarks;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
