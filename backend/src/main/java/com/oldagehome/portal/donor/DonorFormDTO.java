package com.oldagehome.portal.donor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Form-backing DTO for the donor add / edit form.
 *
 * Using a DTO rather than the {@link Donor} entity directly allows:
 *  1. Clean Thymeleaf indexed-list binding for medicine/food sub-tables.
 *  2. Conditional validation (amount only required for non-medicine/food types).
 *  3. Isolation from JPA lazy-loading concerns during form binding.
 */
@Data
public class DonorFormDTO {

    private Long id;

    // ── Personal Information ──────────────────────────────────────────────────

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Gender is required")
    private String gender;

    @NotNull(message = "Date of Birth is required")
    @Past(message = "Date of Birth must be in the past")
    private LocalDate dateOfBirth;

    @Pattern(regexp = "^$|[0-9]{10,15}$", message = "Mobile number must be between 10 and 15 digits")
    private String mobile;

    @Email(message = "Please provide a valid email address")
    private String email;

    private String address;
    private String city;
    private String state;

    @Pattern(regexp = "^$|[0-9]{6}$", message = "Pincode must be exactly 6 digits")
    private String pincode;

    // ── Donation Information ──────────────────────────────────────────────────

    @NotNull(message = "Donation frequency is required")
    private DonationFrequency donationFrequency;

    @NotNull(message = "Donation type is required")
    private DonationType donationType;

    @NotBlank(message = "Donation category is required")
    private String donationCategory;

    // Conditionally required — validated in the controller for CASH/UPI/CHEQUE/GOODS/OTHER
    @DecimalMin(value = "0.0", inclusive = true, message = "Donation amount cannot be negative")
    private BigDecimal donationAmount;

    @NotNull(message = "Donation date is required")
    private LocalDate donationDate;

    private String paymentMethod;
    private String transactionId;
    private String remarks;

    // ── Status & Photo ────────────────────────────────────────────────────────

    @NotNull(message = "Status is required")
    @Builder.Default
    private DonorStatus status = DonorStatus.ACTIVE;

    private String photo;

    // ── Medicine Items (used when donationType == MEDICINE) ───────────────────

    @Valid
    @Builder.Default
    private List<MedicineItemDTO> medicineItems = new ArrayList<>();

    // ── Food Items (used when donationType == FOOD) ───────────────────────────

    @Valid
    @Builder.Default
    private List<FoodItemDTO> foodItems = new ArrayList<>();

    public DonorFormDTO() {
    }

    @Builder
    public DonorFormDTO(Long id, String fullName, String gender, LocalDate dateOfBirth, String mobile,
                        String email, String address, String city, String state, String pincode,
                        DonationFrequency donationFrequency, DonationType donationType, String donationCategory,
                        BigDecimal donationAmount, LocalDate donationDate, String paymentMethod,
                        String transactionId, String remarks, DonorStatus status, String photo,
                        List<MedicineItemDTO> medicineItems, List<FoodItemDTO> foodItems) {
        this.id = id;
        this.fullName = fullName;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.mobile = mobile;
        this.email = email;
        this.address = address;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
        this.donationFrequency = donationFrequency;
        this.donationType = donationType;
        this.donationCategory = donationCategory;
        this.donationAmount = donationAmount;
        this.donationDate = donationDate;
        this.paymentMethod = paymentMethod;
        this.transactionId = transactionId;
        this.remarks = remarks;
        this.status = status != null ? status : DonorStatus.ACTIVE;
        this.photo = photo;
        this.medicineItems = medicineItems != null ? medicineItems : new ArrayList<>();
        this.foodItems = foodItems != null ? foodItems : new ArrayList<>();
    }

    // ── Inner DTOs ────────────────────────────────────────────────────────────

    @Data
    public static class MedicineItemDTO {
        private Long id; // null for new rows
        private String medicineName;
        private BigDecimal price;
        private LocalDate expiryDate;

        public MedicineItemDTO() {
        }

        @Builder
        public MedicineItemDTO(Long id, String medicineName, BigDecimal price, LocalDate expiryDate) {
            this.id = id;
            this.medicineName = medicineName;
            this.price = price;
            this.expiryDate = expiryDate;
        }
    }

    @Data
    public static class FoodItemDTO {
        private Long id; // null for new rows
        private String foodName;
        private String quantity;

        public FoodItemDTO() {
        }

        @Builder
        public FoodItemDTO(Long id, String foodName, String quantity) {
            this.id = id;
            this.foodName = foodName;
            this.quantity = quantity;
        }
    }
}
