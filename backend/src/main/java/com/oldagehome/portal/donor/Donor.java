package com.oldagehome.portal.donor;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

@Entity
@Table(name = "donors")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Donor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @NotBlank(message = "Gender is required")
    @Column(nullable = false, length = 10)
    private String gender; // MALE, FEMALE, OTHER

    @NotNull(message = "Date of Birth is required")
    @Past(message = "Date of Birth must be in the past")
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Min(value = 0, message = "Age cannot be negative")
    private Integer age;

    @Pattern(regexp = "^$|[0-9]{10,15}$", message = "Mobile number must be between 10 and 15 digits")
    @Column(length = 15)
    private String mobile;

    @Email(message = "Please provide a valid email address")
    @Column(length = 100)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Pattern(regexp = "^$|[0-9]{6}$", message = "Pincode must be exactly 6 digits")
    @Column(length = 10)
    private String pincode;

    @NotNull(message = "Donation type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "donation_type", nullable = false, length = 20)
    private DonationType donationType;

    @NotNull(message = "Donation amount is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Donation amount cannot be negative")
    @Column(name = "donation_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal donationAmount;

    @NotNull(message = "Donation date is required")
    @Column(name = "donation_date", nullable = false)
    private LocalDate donationDate;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod; // CASH, UPI, etc.

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DonorStatus status = DonorStatus.ACTIVE;

    @Column(length = 255)
    private String photo; // Filepath of the uploaded image

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Age calculation callback hook
    @PrePersist
    @PreUpdate
    public void calculateAge() {
        if (dateOfBirth != null) {
            this.age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        }
    }
}
