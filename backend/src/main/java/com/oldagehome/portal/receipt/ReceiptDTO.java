package com.oldagehome.portal.receipt;

import com.oldagehome.portal.donor.DonationType;
import lombok.Builder;
import lombok.Data;

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

    public ReceiptDTO() {
    }

    @Builder
    public ReceiptDTO(String receiptNumber, DonationType donationType, Long donorId, Long donationId,
                      String donorName, String address, String city, String state, String pincode,
                      String email, String mobile, String idProof, LocalDate birthDate, LocalDate marriageDate,
                      LocalDate donationDate, String paymentMethod, String transactionId, BigDecimal donationAmount,
                      String amountInWords, String remarks, List<ReceiptItemDTO> items, String backUrl) {
        this.receiptNumber = receiptNumber;
        this.donationType = donationType;
        this.donorId = donorId;
        this.donationId = donationId;
        this.donorName = donorName;
        this.address = address;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
        this.email = email;
        this.mobile = mobile;
        this.idProof = idProof;
        this.birthDate = birthDate;
        this.marriageDate = marriageDate;
        this.donationDate = donationDate;
        this.paymentMethod = paymentMethod;
        this.transactionId = transactionId;
        this.donationAmount = donationAmount;
        this.amountInWords = amountInWords;
        this.remarks = remarks;
        this.items = items != null ? items : new ArrayList<>();
        this.backUrl = backUrl;
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Data
    public static class ReceiptItemDTO {
        private String particulars;
        private String quantity;

        public ReceiptItemDTO() {
        }

        @Builder
        public ReceiptItemDTO(String particulars, String quantity) {
            this.particulars = particulars;
            this.quantity = quantity;
        }
    }
}
