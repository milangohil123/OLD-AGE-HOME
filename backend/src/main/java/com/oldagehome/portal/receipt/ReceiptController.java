package com.oldagehome.portal.receipt;

import com.oldagehome.portal.donor.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles receipt preview routes.
 * GET /receipt/donation/{donationId}  — receipt from a Donation row (history)
 * GET /receipt/donor/{donorId}        — receipt from a Donor's own legacy fields
 *
 * No DB writes — purely reads existing data.
 */
@Controller
@RequestMapping("/receipt")
public class ReceiptController {

    private final DonationRepository donationRepository;
    private final DonorRepository    donorRepository;

    @Autowired
    public ReceiptController(DonationRepository donationRepository,
                             DonorRepository donorRepository) {
        this.donationRepository = donationRepository;
        this.donorRepository    = donorRepository;
    }

    // -------------------------------------------------------------------------
    // GET /receipt/donation/{donationId}
    // Builds receipt from a Donation record (Donation History rows)
    // -------------------------------------------------------------------------
    @GetMapping("/donation/{donationId}")
    public String receiptForDonation(@PathVariable Long donationId, Model model) {

        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new RuntimeException("Donation not found: " + donationId));

        Donor donor = donation.getDonor();
        DonationType type = donation.getDonationType();

        ReceiptDTO dto = buildFromDonation(donation, donor, type);
        dto.setBackUrl("/donors/view/" + donor.getId());

        model.addAttribute("receipt", dto);

        // Route to the correct template
        if (isGiftType(type)) {
            padItemsTo10(dto);
            return "receipt/gift-receipt";
        }
        return "receipt/money-receipt";
    }

    // -------------------------------------------------------------------------
    // GET /receipt/donor/{donorId}
    // Builds receipt from the Donor entity's own legacy donation fields
    // (used by the top "Donation Details" card on the profile page)
    // -------------------------------------------------------------------------
    @GetMapping("/donor/{donorId}")
    public String receiptForDonor(@PathVariable Long donorId, Model model) {

        Donor donor = donorRepository.findById(donorId)
                .orElseThrow(() -> new RuntimeException("Donor not found: " + donorId));

        DonationType type = donor.getDonationType();

        ReceiptDTO dto = buildFromDonor(donor, type);
        dto.setBackUrl("/donors/view/" + donorId);

        model.addAttribute("receipt", dto);

        if (isGiftType(type)) {
            padItemsTo10(dto);
            return "receipt/gift-receipt";
        }
        return "receipt/money-receipt";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isGiftType(DonationType type) {
        return type == DonationType.FOOD || type == DonationType.MEDICINE;
    }

    /** Build ReceiptDTO from a Donation record. */
    private ReceiptDTO buildFromDonation(Donation donation, Donor donor, DonationType type) {
        int year = donation.getDonationDate() != null
                ? donation.getDonationDate().getYear()
                : Year.now().getValue();

        String receiptNum = isGiftType(type)
                ? String.format("GR-%d-%06d", year, donation.getId())
                : String.format("MR-%d-%06d", year, donation.getId());

        List<ReceiptDTO.ReceiptItemDTO> items = buildItems(type,
                donation.getFoodItems(), donation.getMedicineItems());

        return ReceiptDTO.builder()
                .receiptNumber(receiptNum)
                .donationType(type)
                .donorId(donor.getId())
                .donationId(donation.getId())
                .donorName(donor.getFullName())
                .address(buildAddress(donor))
                .city(donor.getCity())
                .state(donor.getState())
                .pincode(donor.getPincode())
                .email(donor.getEmail())
                .mobile(donor.getMobile())
                .idProof("")
                .birthDate(donor.getDateOfBirth())
                .donationDate(donation.getDonationDate())
                .paymentMethod(donation.getPaymentMethod())
                .transactionId(donation.getTransactionId())
                .donationAmount(donation.getDonationAmount())
                .amountInWords(amountToWords(donation.getDonationAmount()))
                .remarks(donation.getRemarks())
                .items(items)
                .build();
    }

    /** Build ReceiptDTO from the Donor entity's own legacy fields. */
    private ReceiptDTO buildFromDonor(Donor donor, DonationType type) {
        int year = donor.getDonationDate() != null
                ? donor.getDonationDate().getYear()
                : Year.now().getValue();

        // Use donorId as the receipt number seed for legacy records
        String receiptNum = isGiftType(type)
                ? String.format("GR-%d-D%05d", year, donor.getId())
                : String.format("MR-%d-D%05d", year, donor.getId());

        List<ReceiptDTO.ReceiptItemDTO> items = buildItems(type,
                donor.getFoodItems(), donor.getMedicineItems());

        return ReceiptDTO.builder()
                .receiptNumber(receiptNum)
                .donationType(type)
                .donorId(donor.getId())
                .donationId(null)
                .donorName(donor.getFullName())
                .address(buildAddress(donor))
                .city(donor.getCity())
                .state(donor.getState())
                .pincode(donor.getPincode())
                .email(donor.getEmail())
                .mobile(donor.getMobile())
                .idProof("")
                .birthDate(donor.getDateOfBirth())
                .donationDate(donor.getDonationDate())
                .paymentMethod(donor.getPaymentMethod())
                .transactionId(donor.getTransactionId())
                .donationAmount(donor.getDonationAmount())
                .amountInWords(amountToWords(donor.getDonationAmount()))
                .remarks(donor.getRemarks())
                .items(items)
                .build();
    }

    private String buildAddress(Donor donor) {
        StringBuilder sb = new StringBuilder();
        if (donor.getAddress() != null && !donor.getAddress().isBlank()) sb.append(donor.getAddress());
        if (donor.getCity()    != null && !donor.getCity().isBlank())    sb.append(sb.length() > 0 ? ", " : "").append(donor.getCity());
        if (donor.getState()   != null && !donor.getState().isBlank())   sb.append(sb.length() > 0 ? ", " : "").append(donor.getState());
        if (donor.getPincode() != null && !donor.getPincode().isBlank()) sb.append(sb.length() > 0 ? " - " : "").append(donor.getPincode());
        return sb.toString();
    }

    private List<ReceiptDTO.ReceiptItemDTO> buildItems(
            DonationType type,
            List<FoodDonationItem> foodItems,
            List<MedicineDonationItem> medItems) {

        List<ReceiptDTO.ReceiptItemDTO> list = new ArrayList<>();
        if (type == DonationType.FOOD && foodItems != null) {
            for (FoodDonationItem f : foodItems) {
                list.add(new ReceiptDTO.ReceiptItemDTO(f.getFoodName(), f.getQuantity()));
            }
        } else if (type == DonationType.MEDICINE && medItems != null) {
            for (MedicineDonationItem m : medItems) {
                String qty = m.getPrice() != null
                        ? "₹" + m.getPrice() + (m.getExpiryDate() != null ? " (Exp: " + m.getExpiryDate() + ")" : "")
                        : "";
                list.add(new ReceiptDTO.ReceiptItemDTO(m.getMedicineName(), qty));
            }
        }
        return list;
    }

    /** Ensure the items list has exactly 10 entries for the Gift Receipt table. */
    private void padItemsTo10(ReceiptDTO dto) {
        List<ReceiptDTO.ReceiptItemDTO> items = dto.getItems();
        while (items.size() < 10) {
            items.add(new ReceiptDTO.ReceiptItemDTO("", ""));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Amount → Indian words conversion
    // ─────────────────────────────────────────────────────────────────────────

    private static final String[] ONES = {
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen",
        "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };
    private static final String[] TENS = {
        "", "", "Twenty", "Thirty", "Forty", "Fifty",
        "Sixty", "Seventy", "Eighty", "Ninety"
    };

    public String amountToWords(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return "Zero Rupees Only";
        long rupees = amount.longValue();
        int paise   = amount.remainder(BigDecimal.ONE).multiply(new BigDecimal(100)).intValue();

        String result = convertToWords(rupees) + " Rupee" + (rupees == 1 ? "" : "s");
        if (paise > 0) {
            result += " and " + convertToWords(paise) + " Paise";
        }
        return result + " Only";
    }

    private String convertToWords(long n) {
        if (n < 0)     return "Minus " + convertToWords(-n);
        if (n == 0)    return "";
        if (n < 20)    return ONES[(int) n];
        if (n < 100)   return TENS[(int) (n / 10)] + (n % 10 != 0 ? " " + ONES[(int) (n % 10)] : "");
        if (n < 1000)  return ONES[(int) (n / 100)] + " Hundred" + (n % 100 != 0 ? " " + convertToWords(n % 100) : "");
        if (n < 100000) return convertToWords(n / 1000) + " Thousand" + (n % 1000 != 0 ? " " + convertToWords(n % 1000) : "");
        if (n < 10000000) return convertToWords(n / 100000) + " Lakh" + (n % 100000 != 0 ? " " + convertToWords(n % 100000) : "");
        return convertToWords(n / 10000000) + " Crore" + (n % 10000000 != 0 ? " " + convertToWords(n % 10000000) : "");
    }
}
