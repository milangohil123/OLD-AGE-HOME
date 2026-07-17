package com.oldagehome.portal.donor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
}
