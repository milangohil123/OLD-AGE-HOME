package com.oldagehome.portal.foodschedule;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Stores configurable donation rates per meal type and sponsorship type.
 * Maps to the food_donation_rates table in Supabase PostgreSQL.
 * Amounts are fetched dynamically from this table — never hardcoded in Java.
 */
@Entity
@Table(name = "food_donation_rates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodDonationRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Stored as plain String to match the DB values exactly
     * (e.g. "Breakfast", "Lunch", "Today's Sponsor").
     */
    @Column(name = "meal_type", nullable = false, length = 30)
    private String mealType;

    /**
     * Stored as plain String to match DB values
     * (e.g. "One Time", "1 Day", "5 Year Tithi").
     */
    @Column(name = "sponsorship_type", nullable = false, length = 30)
    private String sponsorshipType;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
