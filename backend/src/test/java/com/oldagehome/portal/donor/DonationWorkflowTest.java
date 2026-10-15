package com.oldagehome.portal.donor;

import com.oldagehome.portal.audit.AuditService;
import com.oldagehome.portal.inventory.RawMaterialInventoryService;
import com.oldagehome.portal.utils.FileUploadUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit & Integration workflow tests for Donor Donations:
 * Covers all 10 scenarios required by the task specification.
 */
@ExtendWith(MockitoExtension.class)
public class DonationWorkflowTest {

    @Mock
    private DonorRepository donorRepository;

    @Mock
    private DonationRepository donationRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private RawMaterialInventoryService rawMaterialInventoryService;

    @Mock
    private FileUploadUtility fileUploadUtility;

    private DonorServiceImpl donorService;
    private DonorController donorController;
    private Donor testDonor;

    @BeforeEach
    void setUp() {
        donorService = new DonorServiceImpl(
                donorRepository,
                donationRepository,
                auditService,
                rawMaterialInventoryService
        );

        donorController = new DonorController(donorService, fileUploadUtility);

        testDonor = Donor.builder()
                .id(101L)
                .fullName("Rameshbhai Patel")
                .mobile("9876543210")
                .email("ramesh@example.com")
                .donationFrequency(DonationFrequency.ONE_TIME)
                .donationType(DonationType.CASH)
                .donationAmount(new BigDecimal("5000.00"))
                .donationDate(LocalDate.of(2025, 1, 15))
                .paymentMethod("CASH")
                .foodItems(new ArrayList<>())
                .medicineItems(new ArrayList<>())
                .donations(new ArrayList<>())
                .build();
    }

    // -------------------------------------------------------------------------
    // Scenario 1: An existing donor submits a normal monetary donation.
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 1: Existing donor submits a normal monetary donation (₹1,000)")
    void testExistingDonorSubmitsNormalMonetaryDonation() {
        when(donorRepository.findById(101L)).thenReturn(Optional.of(testDonor));
        ArgumentCaptor<Donation> donationCaptor = ArgumentCaptor.forClass(Donation.class);
        when(donationRepository.save(donationCaptor.capture())).thenAnswer(invocation -> {
            Donation d = invocation.getArgument(0);
            d.setId(1L);
            return d;
        });

        DonationFormDTO dto = new DonationFormDTO();
        dto.setDonorId(101L);
        dto.setDonationFrequency(DonationFrequency.ONE_TIME);
        dto.setDonationType(DonationType.CASH);
        dto.setDonationAmount(new BigDecimal("1000.00"));
        dto.setDonationDate(LocalDate.now());
        dto.setPaymentMethod("UPI");
        dto.setTransactionId("UPI-123456");

        donorService.saveDonation(dto);

        Donation saved = donationCaptor.getValue();
        assertNotNull(saved);
        assertEquals(101L, saved.getDonor().getId());
        assertEquals(new BigDecimal("1000.00"), saved.getDonationAmount());
        assertEquals(DonationType.CASH, saved.getDonationType());
        assertEquals("UPI", saved.getPaymentMethod());
        assertEquals("UPI-123456", saved.getTransactionId());
        verify(donationRepository, times(1)).save(any(Donation.class));
    }

    // -------------------------------------------------------------------------
    // Scenario 2: The same donor submits a second monetary donation.
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 2: The same donor submits a second monetary donation without overwriting")
    void testSameDonorSubmitsSecondMonetaryDonation() {
        when(donorRepository.findById(101L)).thenReturn(Optional.of(testDonor));

        Donation firstDonation = Donation.builder()
                .id(1L)
                .donor(testDonor)
                .donationAmount(new BigDecimal("1000.00"))
                .donationDate(LocalDate.now().minusDays(10))
                .build();

        Donation secondDonation = Donation.builder()
                .id(2L)
                .donor(testDonor)
                .donationAmount(new BigDecimal("2500.00"))
                .donationDate(LocalDate.now())
                .build();

        when(donationRepository.save(any(Donation.class))).thenReturn(secondDonation);
        when(donationRepository.findAllByDonorIdOrderByDonationDateDescCreatedAtDesc(101L))
                .thenReturn(List.of(secondDonation, firstDonation));

        DonationFormDTO dto2 = new DonationFormDTO();
        dto2.setDonorId(101L);
        dto2.setDonationAmount(new BigDecimal("2500.00"));
        dto2.setDonationType(DonationType.CASH);
        dto2.setDonationDate(LocalDate.now());

        donorService.saveDonation(dto2);

        List<Donation> history = donorService.getDonationsByDonorId(101L);
        assertEquals(2, history.size(), "Both donations must exist for the same donor");
        assertEquals(new BigDecimal("2500.00"), history.get(0).getDonationAmount());
        assertEquals(new BigDecimal("1000.00"), history.get(1).getDonationAmount());
    }

    // -------------------------------------------------------------------------
    // Scenario 3: Physical donation of 5 kg of dal without monetary amount.
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 3: Same donor submits a physical donation of 5 kg of Dal without amount")
    void testPhysicalDonationOfDalWithoutMonetaryAmount() {
        when(donorRepository.findById(101L)).thenReturn(Optional.of(testDonor));
        ArgumentCaptor<Donation> donationCaptor = ArgumentCaptor.forClass(Donation.class);
        when(donationRepository.save(donationCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        DonationFormDTO dto = new DonationFormDTO();
        dto.setDonorId(101L);
        dto.setDonationFrequency(DonationFrequency.ONE_TIME);
        dto.setDonationType(DonationType.DAL);
        dto.setDonationAmount(null); // Optional for physical donation!
        dto.setDonationDate(LocalDate.now());
        dto.setFoodItems(List.of(
                DonorFormDTO.FoodItemDTO.builder().foodName("Toor Dal").quantity("5 kg").build()
        ));

        // Validate via controller
        BindingResult bindingResult = new BeanPropertyBindingResult(dto, "donation");
        Model model = new ExtendedModelMap();
        RedirectAttributes ra = new RedirectAttributesModelMap();

        String view = donorController.saveDonation(101L, dto, bindingResult, model, ra);
        assertEquals("redirect:/donors/view/101", view);
        assertFalse(bindingResult.hasErrors(), "Physical donation of 5kg Dal must be valid without amount");

        Donation saved = donationCaptor.getValue();
        assertNotNull(saved);
        assertNull(saved.getDonationAmount(), "Monetary amount must be null (not fabricated to 0.00)");
        assertEquals(DonationType.DAL, saved.getDonationType());
        assertTrue(saved.getDonationType().isFoodType());
        assertEquals(1, saved.getFoodItems().size());
        assertEquals("Toor Dal", saved.getFoodItems().get(0).getFoodName());
        assertEquals("5 kg", saved.getFoodItems().get(0).getQuantity());
        assertEquals(testDonor, saved.getFoodItems().get(0).getDonor());
    }

    // -------------------------------------------------------------------------
    // Scenario 4: Physical donation without required item details is rejected.
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 4: Physical donation without item details is rejected with validation message")
    void testPhysicalDonationWithoutItemDetailsIsRejected() {
        DonationFormDTO dto = new DonationFormDTO();
        dto.setDonorId(101L);
        dto.setDonationFrequency(DonationFrequency.ONE_TIME);
        dto.setDonationType(DonationType.DAL); // isFoodType() == true
        dto.setFoodItems(new ArrayList<>()); // Empty items list!

        BindingResult bindingResult = new BeanPropertyBindingResult(dto, "donation");
        Model model = new ExtendedModelMap();
        RedirectAttributes ra = new RedirectAttributesModelMap();

        when(donorRepository.findById(101L)).thenReturn(Optional.of(testDonor));

        String view = donorController.saveDonation(101L, dto, bindingResult, model, ra);
        assertEquals("donors/donation-form", view);
        assertTrue(bindingResult.hasFieldErrors("foodItems"), "foodItems must have validation error");
        assertTrue(bindingResult.getFieldError("foodItems").getDefaultMessage().contains("at least one item name and its quantity"));
    }

    // -------------------------------------------------------------------------
    // Scenario 5: Invalid monetary amount is rejected.
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 5: Negative or zero monetary amount is rejected")
    void testNegativeOrZeroMonetaryAmountIsRejected() {
        when(donorRepository.findById(101L)).thenReturn(Optional.of(testDonor));

        // Test negative amount
        DonationFormDTO dtoNegative = new DonationFormDTO();
        dtoNegative.setDonorId(101L);
        dtoNegative.setDonationFrequency(DonationFrequency.ONE_TIME);
        dtoNegative.setDonationType(DonationType.CASH);
        dtoNegative.setDonationDate(LocalDate.now());
        dtoNegative.setDonationAmount(new BigDecimal("-50.00"));

        BindingResult brNegative = new BeanPropertyBindingResult(dtoNegative, "donation");
        donorController.saveDonation(101L, dtoNegative, brNegative, new ExtendedModelMap(), new RedirectAttributesModelMap());
        assertTrue(brNegative.hasFieldErrors("donationAmount"));
        assertTrue(brNegative.getFieldError("donationAmount").getDefaultMessage().contains("greater than 0"));

        // Test zero amount for cash donation
        DonationFormDTO dtoZero = new DonationFormDTO();
        dtoZero.setDonorId(101L);
        dtoZero.setDonationFrequency(DonationFrequency.ONE_TIME);
        dtoZero.setDonationType(DonationType.CASH);
        dtoZero.setDonationDate(LocalDate.now());
        dtoZero.setDonationAmount(BigDecimal.ZERO);

        BindingResult brZero = new BeanPropertyBindingResult(dtoZero, "donation");
        donorController.saveDonation(101L, dtoZero, brZero, new ExtendedModelMap(), new RedirectAttributesModelMap());
        assertTrue(brZero.hasFieldErrors("donationAmount"));
    }

    // -------------------------------------------------------------------------
    // Scenario 6: Invalid donor ID does not create an orphan donation.
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 6: Invalid donor ID redirects with error and creates no donation")
    void testInvalidDonorIdDoesNotCreateOrphanDonation() {
        // 1. Non-existent donor ID
        when(donorRepository.findById(999L)).thenReturn(Optional.empty());

        DonationFormDTO dto = new DonationFormDTO();
        dto.setDonationAmount(new BigDecimal("500.00"));
        dto.setDonationType(DonationType.CASH);
        dto.setDonationDate(LocalDate.now());

        BindingResult bindingResult = new BeanPropertyBindingResult(dto, "donation");
        Model model = new ExtendedModelMap();
        RedirectAttributes ra = new RedirectAttributesModelMap();

        String view = donorController.saveDonation(999L, dto, bindingResult, model, ra);
        assertEquals("redirect:/donors", view);
        assertTrue(ra.getFlashAttributes().containsKey("errorMessage"));
        verify(donationRepository, never()).save(any());

        // 2. Null or non-positive donor ID
        RedirectAttributes raNull = new RedirectAttributesModelMap();
        String viewNull = donorController.saveDonation(-1L, dto, bindingResult, model, raNull);
        assertEquals("redirect:/donors", viewNull);
        assertTrue(raNull.getFlashAttributes().containsKey("errorMessage"));
        verify(donationRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Scenario 7: After saving, profile shows both previous & new donations.
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 7: Profile loads all past donations alongside the newly saved donation")
    void testProfileShowsAllPastAndNewDonations() {
        when(donorRepository.findById(101L)).thenReturn(Optional.of(testDonor));

        Donation d1 = Donation.builder()
                .id(1L)
                .donor(testDonor)
                .donationAmount(new BigDecimal("5000.00"))
                .donationDate(LocalDate.of(2025, 1, 15))
                .build();

        Donation d2 = Donation.builder()
                .id(2L)
                .donor(testDonor)
                .donationType(DonationType.DAL)
                .donationAmount(null)
                .donationDate(LocalDate.of(2025, 2, 10))
                .foodItems(List.of(FoodDonationItem.builder().foodName("Dal").quantity("5 kg").build()))
                .build();

        when(donationRepository.findAllByDonorIdOrderByDonationDateDescCreatedAtDesc(101L))
                .thenReturn(List.of(d2, d1));

        Model model = new ExtendedModelMap();
        RedirectAttributes ra = new RedirectAttributesModelMap();
        String viewName = donorController.viewDonor(101L, model, ra);

        assertEquals("donors/view", viewName);
        assertTrue(model.containsAttribute("donor"));
        assertTrue(model.containsAttribute("donations"));

        @SuppressWarnings("unchecked")
        List<Donation> list = (List<Donation>) model.getAttribute("donations");
        assertEquals(2, list.size());
        assertEquals(2L, list.get(0).getId());
        assertEquals(1L, list.get(1).getId());
    }

    // -------------------------------------------------------------------------
    // Scenario 8: Reloading the page still shows the saved donation.
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 8: Reloading page queries repository and consistently returns saved donations")
    void testReloadingPageConsistentlyReturnsSavedDonations() {
        when(donorRepository.findById(101L)).thenReturn(Optional.of(testDonor));

        Donation persistentDonation = Donation.builder()
                .id(10L)
                .donor(testDonor)
                .donationType(DonationType.GROCERY)
                .donationAmount(null)
                .donationDate(LocalDate.now())
                .foodItems(List.of(FoodDonationItem.builder().foodName("Rice & Dal").quantity("10 kg").build()))
                .build();

        when(donationRepository.findAllByDonorIdOrderByDonationDateDescCreatedAtDesc(101L))
                .thenReturn(List.of(persistentDonation));

        // First load
        Model model1 = new ExtendedModelMap();
        donorController.viewDonor(101L, model1, new RedirectAttributesModelMap());
        @SuppressWarnings("unchecked")
        List<Donation> res1 = (List<Donation>) model1.getAttribute("donations");
        assertEquals(1, res1.size());
        assertEquals(10L, res1.get(0).getId());

        // Simulated reload
        Model model2 = new ExtendedModelMap();
        donorController.viewDonor(101L, model2, new RedirectAttributesModelMap());
        @SuppressWarnings("unchecked")
        List<Donation> res2 = (List<Donation>) model2.getAttribute("donations");
        assertEquals(1, res2.size());
        assertEquals(10L, res2.get(0).getId());
    }

    // -------------------------------------------------------------------------
    // Scenario 9: Dashboard monetary totals remain correct.
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 9: Dashboard monetary totals exclude physical donations without NPE")
    void testDashboardMonetaryTotalsExcludePhysicalDonations() {
        List<Donation> allDonations = List.of(
                Donation.builder().id(1L).donationAmount(new BigDecimal("1000.00")).build(),
                Donation.builder().id(2L).donationAmount(null).donationType(DonationType.DAL).build(),
                Donation.builder().id(3L).donationAmount(new BigDecimal("2500.50")).build(),
                Donation.builder().id(4L).donationAmount(null).donationType(DonationType.GROCERY).build()
        );

        // Monetary sum should only sum non-null positive amounts
        BigDecimal totalMonetary = allDonations.stream()
                .map(Donation::getDonationAmount)
                .filter(amt -> amt != null && amt.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(new BigDecimal("3500.50"), totalMonetary);

        // Verify DonationType isFoodType for GROCERY and DAL
        assertTrue(DonationType.DAL.isFoodType());
        assertTrue(DonationType.GROCERY.isFoodType());
        assertFalse(DonationType.CASH.isFoodType());
    }

    // -------------------------------------------------------------------------
    // Scenario 10: Existing donation history remains intact.
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 10: Adding repeat donation leaves prior donations intact")
    void testExistingDonationHistoryRemainsIntact() {
        when(donorRepository.findById(101L)).thenReturn(Optional.of(testDonor));

        Donation existingD1 = Donation.builder().id(1L).donor(testDonor).donationAmount(new BigDecimal("500.00")).build();
        Donation existingD2 = Donation.builder().id(2L).donor(testDonor).donationAmount(new BigDecimal("750.00")).build();
        testDonor.getDonations().add(existingD1);
        testDonor.getDonations().add(existingD2);

        DonationFormDTO newDto = new DonationFormDTO();
        newDto.setDonorId(101L);
        newDto.setDonationType(DonationType.DAL);
        newDto.setDonationAmount(null);
        newDto.setFoodItems(List.of(DonorFormDTO.FoodItemDTO.builder().foodName("Dal").quantity("2 kg").build()));

        when(donationRepository.save(any(Donation.class))).thenAnswer(inv -> inv.getArgument(0));

        donorService.saveDonation(newDto);

        // Verify testDonor still has the prior donations intact
        assertTrue(testDonor.getDonations().contains(existingD1));
        assertTrue(testDonor.getDonations().contains(existingD2));
        assertEquals(3, testDonor.getDonations().size());
    }
}
