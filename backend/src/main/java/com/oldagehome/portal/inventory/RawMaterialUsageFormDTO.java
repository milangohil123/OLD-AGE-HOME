package com.oldagehome.portal.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class RawMaterialUsageFormDTO {

    @NotNull(message = "Usage Date is required")
    private LocalDate usageDate = LocalDate.now();

    @NotBlank(message = "Purpose is required")
    private String purpose;

    @NotEmpty(message = "At least one item must be logged")
    @Valid
    private List<UsageItemDTO> items = new ArrayList<>();

    @Data
    public static class UsageItemDTO {
        @NotBlank(message = "Raw material name is required")
        private String rawMaterialName;

        @NotNull(message = "Quantity used is required")
        private BigDecimal quantityUsed;

        @NotBlank(message = "Unit is required")
        private String unit;
    }
}
