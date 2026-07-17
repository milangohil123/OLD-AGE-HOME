package com.oldagehome.portal.receipt;

import com.oldagehome.portal.donor.DonationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for receipt preview pages.
 * Used by both Money Receipt and Gift Receipt templates.
 * No @Entity — purely a view/presentation DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiptDTO {

    // ── Receipt Meta ──────────────────────────────────────────────────────────
    private String receiptNumber;    // e.g. MR-2026-000042 or GR-2026-000042
    private DonationType donationType;
    private Long donorId;
    private Long donationId;         // null for legacy donor-level receipt

    // ── Donor / Personal ──────────────────────────────────────────────────────
    private String donorName;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String email;
    private String mobile;
    private String idProof;          // Aadhaar / PAN — field exists on form only
    private LocalDate birthDate;
    private LocalDate marriageDate;  // Optional

    // ── Donation ──────────────────────────────────────────────────────────────
    private LocalDate donationDate;
    private String paymentMethod;
    private String transactionId;
    private BigDecimal donationAmount;
    private String amountInWords;
    private String remarks;

    // ── Items (Food / Medicine rows) ─────────────────────────────────────────
    @Builder.Default
    private List<ReceiptItemDTO> items = new ArrayList<>();

    // ── Back-link ─────────────────────────────────────────────────────────────
    private String backUrl;

    // ─────────────────────────────────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReceiptItemDTO {
        private String particulars;
        private String quantity;
    }
}
