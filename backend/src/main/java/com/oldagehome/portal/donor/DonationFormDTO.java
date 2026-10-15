package com.oldagehome.portal.donor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class DonationFormDTO {

    private Long id;
    private Long donorId;

    @NotNull(message = "Donation frequency is required")
    private DonationFrequency donationFrequency;

    @NotNull(message = "Donation type is required")
    private DonationType donationType;

    @DecimalMin(value = "0.0", inclusive = true, message = "Donation amount cannot be negative")
    private BigDecimal donationAmount;

    @NotNull(message = "Donation date is required")
    private LocalDate donationDate;

    private String paymentMethod;
    private String transactionId;
    private String remarks;

    @Valid
    @Builder.Default
    private List<DonorFormDTO.MedicineItemDTO> medicineItems = new ArrayList<>();

    @Valid
    @Builder.Default
    private List<DonorFormDTO.FoodItemDTO> foodItems = new ArrayList<>();

    public DonationFormDTO() {
    }

    @Builder
    public DonationFormDTO(Long id, Long donorId, DonationFrequency donationFrequency, DonationType donationType,
                           BigDecimal donationAmount, LocalDate donationDate, String paymentMethod,
                           String transactionId, String remarks, List<DonorFormDTO.MedicineItemDTO> medicineItems,
                           List<DonorFormDTO.FoodItemDTO> foodItems) {
        this.id = id;
        this.donorId = donorId;
        this.donationFrequency = donationFrequency;
        this.donationType = donationType;
        this.donationAmount = donationAmount;
        this.donationDate = donationDate;
        this.paymentMethod = paymentMethod;
        this.transactionId = transactionId;
        this.remarks = remarks;
        this.medicineItems = medicineItems != null ? medicineItems : new ArrayList<>();
        this.foodItems = foodItems != null ? foodItems : new ArrayList<>();
    }
}
