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
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "donations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donor_id", nullable = false)
    private Donor donor;

    @Enumerated(EnumType.STRING)
    @Column(name = "donation_frequency", length = 20)
    private DonationFrequency donationFrequency;

    @Enumerated(EnumType.STRING)
    @Column(name = "donation_type", length = 20)
    private DonationType donationType;

    @Column(name = "donation_amount", precision = 10, scale = 2)
    private BigDecimal donationAmount;

    @Column(name = "donation_date")
    private LocalDate donationDate;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @OneToMany(mappedBy = "donation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<MedicineDonationItem> medicineItems = new ArrayList<>();

    @OneToMany(mappedBy = "donation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<FoodDonationItem> foodItems = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public double getTotalFoodQuantity() {
        if (foodItems == null || foodItems.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^\\s*([0-9]+(?:\\.[0-9]+)?).*");
        for (FoodDonationItem item : foodItems) {
            if (item.getQuantity() != null) {
                java.util.regex.Matcher matcher = pattern.matcher(item.getQuantity());
                if (matcher.matches()) {
                    try {
                        total += Double.parseDouble(matcher.group(1));
                    } catch (NumberFormatException e) {
                        // Ignore
                    }
                }
            }
        }
        return total;
    }

    public String getFormattedTotalFoodQuantity() {
        double total = getTotalFoodQuantity();
        if (total == (long) total) {
            return String.format("%d", (long) total);
        } else {
            return String.format("%s", total);
        }
    }
}
