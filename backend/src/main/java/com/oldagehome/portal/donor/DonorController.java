package com.oldagehome.portal.donor;

import com.oldagehome.portal.common.AppConstants;
import com.oldagehome.portal.common.PaginationUtils;
import com.oldagehome.portal.dto.DonorImportDTO;
import com.oldagehome.portal.utils.FileUploadUtility;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;

@Controller
@RequestMapping("/donors")
public class DonorController {

    private static final String DONORS_DIR = "donors";

    private final DonorService donorService;
    private final FileUploadUtility fileUploadUtility;

    @Autowired
    public DonorController(DonorService donorService, FileUploadUtility fileUploadUtility) {
        this.donorService = donorService;
        this.fileUploadUtility = fileUploadUtility;
    }

    // -------------------------------------------------------------------------
    // GET /donors — List with pagination and search
    // -------------------------------------------------------------------------

    @GetMapping
    public String listDonors(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = AppConstants.Pagination.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(value = "size", defaultValue = AppConstants.Pagination.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(value = "sort", defaultValue = AppConstants.Pagination.DEFAULT_SORT_BY) String sort,
            @RequestParam(value = "direction", defaultValue = AppConstants.Pagination.DEFAULT_SORT_DIRECTION) String direction,
            Model model) {

        Pageable pageable = buildDonorPageable(page, size, sort, direction);
        Page<Donor> donorPage = donorService.getDonors(keyword, pageable);

        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);
        LinkedHashMap<String, Object> paginationParams = new LinkedHashMap<>();
        paginationParams.put("keyword", keyword);
        paginationParams.put("sort", sort);
        paginationParams.put("direction", direction);
        model.addAttribute("paginationQuery", PaginationUtils.buildQueryString(paginationParams));

        model.addAttribute("donorsPage", donorPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", donorPage.getTotalPages());
        model.addAttribute("pageSize", size);
        model.addAttribute("totalElements", donorPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("activePage", "donors");

        // Dashboard stats for list page header cards
        model.addAttribute("totalDonors", donorService.countTotalDonors());
        model.addAttribute("todayDonations", donorService.countTodayDonations());
        model.addAttribute("monthDonations", donorService.countThisMonthDonations());
        model.addAttribute("totalAmount", donorService.sumTotalDonationAmount());

        return "donors/list";
    }

    // -------------------------------------------------------------------------
    // GET /donors/view/{id} — Donor profile page
    // -------------------------------------------------------------------------

    @GetMapping("/view/{id}")
    public String viewDonor(@PathVariable("id") Long id, Model model) {
        Donor donor = donorService.getDonorById(id);
        model.addAttribute("donor", donor);
        model.addAttribute("activePage", "donors");
        return "donors/view";
    }

    // -------------------------------------------------------------------------
    // GET /donors/new — Show blank registration form
    // -------------------------------------------------------------------------

    @GetMapping("/new")
    public String showNewForm(Model model) {
        DonorFormDTO dto = new DonorFormDTO();
        // Add 3 default empty rows for medicine and food item tables
        for (int i = 0; i < 3; i++) {
            dto.getMedicineItems().add(new DonorFormDTO.MedicineItemDTO());
            dto.getFoodItems().add(new DonorFormDTO.FoodItemDTO());
        }

        model.addAttribute("donor", dto);
        model.addAttribute("donationFrequencies", DonationFrequency.values());
        model.addAttribute("donationTypes", DonationType.values());
        model.addAttribute("donorStatuses", DonorStatus.values());
        model.addAttribute("activePage", "donors");
        model.addAttribute("pageTitle", "Register Donor");

        return "donors/form";
    }

    // -------------------------------------------------------------------------
    // POST /donors/save — Persist new donor
    // -------------------------------------------------------------------------

    @PostMapping("/save")
    public String saveDonor(
            @Valid @ModelAttribute("donor") DonorFormDTO donorFormDto,
            BindingResult bindingResult,
            @RequestParam("photoFile") MultipartFile photoFile,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Validate conditional donation fields (medicine list, food list, amount)
        validateDonorForm(donorFormDto, bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute("donationFrequencies", DonationFrequency.values());
            model.addAttribute("donationTypes", DonationType.values());
            model.addAttribute("donorStatuses", DonorStatus.values());
            model.addAttribute("activePage", "donors");
            model.addAttribute("pageTitle", "Register Donor");
            return "donors/form";
        }

        // Handle photo upload
        try {
            if (!photoFile.isEmpty()) {
                String photoPath = fileUploadUtility.saveFile(DONORS_DIR, photoFile);
                donorFormDto.setPhoto(photoPath);
            }
        } catch (IOException e) {
            bindingResult.reject(AppConstants.Messages.ERROR_GENERIC, "Failed to upload photo file.");
            model.addAttribute("donationFrequencies", DonationFrequency.values());
            model.addAttribute("donationTypes", DonationType.values());
            model.addAttribute("donorStatuses", DonorStatus.values());
            model.addAttribute("activePage", "donors");
            model.addAttribute("pageTitle", "Register Donor");
            return "donors/form";
        }

        donorService.saveDonor(donorFormDto);
        redirectAttributes.addFlashAttribute("successMessage", AppConstants.Messages.SUCCESS_SAVE);
        return "redirect:/donors";
    }

    // -------------------------------------------------------------------------
    // GET /donors/edit/{id} — Show pre-populated edit form
    // -------------------------------------------------------------------------
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        DonorFormDTO dto = donorService.getDonorFormDtoById(id);

        // Ensure at least 3 rows exist in lists to display properly in the UI tables
        while (dto.getMedicineItems().size() < 3) {
            dto.getMedicineItems().add(new DonorFormDTO.MedicineItemDTO());
        }
        while (dto.getFoodItems().size() < 3) {
            dto.getFoodItems().add(new DonorFormDTO.FoodItemDTO());
        }

        model.addAttribute("donor", dto);
        model.addAttribute("donationFrequencies", DonationFrequency.values());
        model.addAttribute("donationTypes", DonationType.values());
        model.addAttribute("donorStatuses", DonorStatus.values());
        model.addAttribute("activePage", "donors");
        model.addAttribute("pageTitle", "Edit Donor");

        return "donors/form";
    }

    // -------------------------------------------------------------------------
    // POST /donors/update — Persist edits
    // -------------------------------------------------------------------------

    @PostMapping("/update")
    public String updateDonor(
            @Valid @ModelAttribute("donor") DonorFormDTO donorFormDto,
            BindingResult bindingResult,
            @RequestParam("photoFile") MultipartFile photoFile,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Validate conditional donation fields
        validateDonorForm(donorFormDto, bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute("donationFrequencies", DonationFrequency.values());
            model.addAttribute("donationTypes", DonationType.values());
            model.addAttribute("donorStatuses", DonorStatus.values());
            model.addAttribute("activePage", "donors");
            model.addAttribute("pageTitle", "Edit Donor");
            return "donors/form";
        }

        // Handle photo update
        try {
            if (!photoFile.isEmpty()) {
                Donor oldDonor = donorService.getDonorById(donorFormDto.getId());
                if (oldDonor.getPhoto() != null) {
                    fileUploadUtility.deleteFile(oldDonor.getPhoto());
                }
                String photoPath = fileUploadUtility.saveFile(DONORS_DIR, photoFile);
                donorFormDto.setPhoto(photoPath);
            }
        } catch (IOException e) {
            bindingResult.reject(AppConstants.Messages.ERROR_GENERIC, "Failed to upload new photo file.");
            model.addAttribute("donationFrequencies", DonationFrequency.values());
            model.addAttribute("donationTypes", DonationType.values());
            model.addAttribute("donorStatuses", DonorStatus.values());
            model.addAttribute("activePage", "donors");
            model.addAttribute("pageTitle", "Edit Donor");
            return "donors/form";
        }

        donorService.updateDonor(donorFormDto);
        redirectAttributes.addFlashAttribute("successMessage", AppConstants.Messages.SUCCESS_UPDATE);
        return "redirect:/donors";
    }

    // -------------------------------------------------------------------------
    // GET /donors/delete/{id} — Delete donor
    // -------------------------------------------------------------------------

    @GetMapping("/delete/{id}")
    public String deleteDonor(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Donor donor = donorService.getDonorById(id);

        // Remove photo file from disk
        if (donor.getPhoto() != null) {
            fileUploadUtility.deleteFile(donor.getPhoto());
        }

        donorService.deleteDonor(id);
        redirectAttributes.addFlashAttribute("successMessage", AppConstants.Messages.SUCCESS_DELETE);
        return "redirect:/donors";
    }

    // -------------------------------------------------------------------------
    // GET /donors/export — Download Excel
    // -------------------------------------------------------------------------

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportToExcel() {
        try {
            byte[] data = donorService.exportToExcel();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=donors.xlsx")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(data);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // -------------------------------------------------------------------------
    // POST /donors/import — Upload Excel
    // -------------------------------------------------------------------------

    @PostMapping("/import")
    public String importExcel(
            @RequestParam("file") MultipartFile file,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Please select a valid Excel file to import.");
            return "redirect:/donors";
        }

        try {
            List<DonorImportDTO> results = donorService.importFromExcel(file);

            long successCount = results.stream().filter(DonorImportDTO::isValid).count();
            long failCount = results.stream().filter(r -> !r.isValid()).count();

            model.addAttribute("importResults", results);
            model.addAttribute("successCount", successCount);
            model.addAttribute("failCount", failCount);
            model.addAttribute("totalCount", results.size());
            model.addAttribute("activePage", "donors");

            return "donors/import_result";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to parse Excel file: " + e.getMessage());
            return "redirect:/donors";
        }
    }

    // -------------------------------------------------------------------------
    // GET /donors/{id}/donations/new — Show add donation form
    // -------------------------------------------------------------------------

    @GetMapping("/{id}/donations/new")
    public String showAddDonationForm(@PathVariable("id") Long id, Model model) {
        Donor donor = donorService.getDonorById(id);
        
        DonationFormDTO dto = new DonationFormDTO();
        dto.setDonorId(donor.getId());
        dto.setDonationDate(java.time.LocalDate.now());
        
        for (int i = 0; i < 3; i++) {
            dto.getMedicineItems().add(new DonorFormDTO.MedicineItemDTO());
            dto.getFoodItems().add(new DonorFormDTO.FoodItemDTO());
        }

        model.addAttribute("donation", dto);
        model.addAttribute("donor", donor);
        model.addAttribute("donationFrequencies", DonationFrequency.values());
        model.addAttribute("donationTypes", DonationType.values());
        model.addAttribute("activePage", "donors");
        model.addAttribute("pageTitle", "Add Donation for " + donor.getFullName());

        return "donors/donation-form";
    }

    // -------------------------------------------------------------------------
    // POST /donors/{id}/donations — Persist new donation
    // -------------------------------------------------------------------------

    @PostMapping("/{id}/donations")
    public String saveDonation(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("donation") DonationFormDTO donationFormDto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        validateDonationForm(donationFormDto, bindingResult);

        if (bindingResult.hasErrors()) {
            Donor donor = donorService.getDonorById(id);
            model.addAttribute("donor", donor);
            model.addAttribute("donationFrequencies", DonationFrequency.values());
            model.addAttribute("donationTypes", DonationType.values());
            model.addAttribute("activePage", "donors");
            model.addAttribute("pageTitle", "Add Donation for " + donor.getFullName());
            return "donors/donation-form";
        }

        donationFormDto.setDonorId(id);
        donorService.saveDonation(donationFormDto);
        redirectAttributes.addFlashAttribute("successMessage", "Donation added successfully.");
        return "redirect:/donors/view/" + id;
    }

    private void validateDonationForm(DonationFormDTO dto, BindingResult bindingResult) {
        if (dto.getDonationFrequency() == null) {
            bindingResult.rejectValue("donationFrequency", "error.donationFrequency", "Donation Frequency is required");
        }
        if (dto.getDonationType() == null) {
            bindingResult.rejectValue("donationType", "error.donationType", "Donation Type is required");
            return;
        }

        if (dto.getDonationType() == DonationType.MEDICINE) {
            List<DonorFormDTO.MedicineItemDTO> items = dto.getMedicineItems();
            boolean hasAtLeastOne = false;
            for (int i = 0; i < items.size(); i++) {
                DonorFormDTO.MedicineItemDTO item = items.get(i);
                boolean hasAnyField = (item.getMedicineName() != null && !item.getMedicineName().isBlank())
                        || item.getPrice() != null
                        || item.getExpiryDate() != null;
                if (hasAnyField) {
                    hasAtLeastOne = true;
                    if (item.getMedicineName() == null || item.getMedicineName().isBlank()) {
                        bindingResult.rejectValue("medicineItems[" + i + "].medicineName", "error.medicineName", "Medicine Name is required");
                    }
                    if (item.getPrice() == null) {
                        bindingResult.rejectValue("medicineItems[" + i + "].price", "error.price", "Price is required");
                    } else if (item.getPrice().compareTo(BigDecimal.ZERO) < 0) {
                        bindingResult.rejectValue("medicineItems[" + i + "].price", "error.price", "Price cannot be negative");
                    }
                    if (item.getExpiryDate() == null) {
                        bindingResult.rejectValue("medicineItems[" + i + "].expiryDate", "error.expiryDate", "Expiry Date is required");
                    }
                }
            }
            if (!hasAtLeastOne) {
                bindingResult.rejectValue("medicineItems", "error.medicineItems", "At least one medicine row is required");
            }
        } else if (dto.getDonationType() == DonationType.FOOD) {
            List<DonorFormDTO.FoodItemDTO> items = dto.getFoodItems();
            boolean hasAtLeastOne = false;
            for (int i = 0; i < items.size(); i++) {
                DonorFormDTO.FoodItemDTO item = items.get(i);
                boolean hasAnyField = (item.getFoodName() != null && !item.getFoodName().isBlank())
                        || (item.getQuantity() != null && !item.getQuantity().isBlank());
                if (hasAnyField) {
                    hasAtLeastOne = true;
                    if (item.getFoodName() == null || item.getFoodName().isBlank()) {
                        bindingResult.rejectValue("foodItems[" + i + "].foodName", "error.foodName", "Food Name is required");
                    }
                    if (item.getQuantity() == null || item.getQuantity().isBlank()) {
                        bindingResult.rejectValue("foodItems[" + i + "].quantity", "error.quantity", "Quantity is required");
                    }
                }
            }
            if (!hasAtLeastOne) {
                bindingResult.rejectValue("foodItems", "error.foodItems", "At least one food row is required");
            }
        } else if (dto.getDonationType() == DonationType.CASH || dto.getDonationType() == DonationType.UPI || dto.getDonationType() == DonationType.CHEQUE) {
            if (dto.getDonationAmount() == null) {
                bindingResult.rejectValue("donationAmount", "error.donationAmount", "Donation Amount is required");
            }
        }
    }

    private Pageable buildDonorPageable(int page, int size, String sort, String direction) {
        String normalizedSort = sort != null ? sort.trim().toLowerCase() : "";
        String property;

        switch (normalizedSort) {
            case "name":
            case "fullname":
            case "full_name":
                property = "fullName";
                break;
            case "amount":
            case "donationamount":
                property = "donationAmount";
                break;
            case "dateadded":
            case "donationdate":
                property = "donationDate";
                break;
            default:
                property = AppConstants.Pagination.DEFAULT_SORT_BY;
                break;
        }

        return PaginationUtils.buildPageable(page, size, property, direction, AppConstants.Pagination.DEFAULT_SORT_BY);
    }

    private void validateDonorForm(DonorFormDTO dto, BindingResult bindingResult) {
        if (dto.getDonationFrequency() == null) {
            bindingResult.rejectValue("donationFrequency", "error.donationFrequency", "Donation Frequency is required");
        }
        if (dto.getDonationType() == null) {
            bindingResult.rejectValue("donationType", "error.donationType", "Donation Type is required");
            return;
        }

        if (dto.getDonationType() == DonationType.MEDICINE) {
            List<DonorFormDTO.MedicineItemDTO> items = dto.getMedicineItems();
            boolean hasAtLeastOne = false;
            for (int i = 0; i < items.size(); i++) {
                DonorFormDTO.MedicineItemDTO item = items.get(i);
                boolean hasAnyField = (item.getMedicineName() != null && !item.getMedicineName().isBlank())
                        || item.getPrice() != null
                        || item.getExpiryDate() != null;
                if (hasAnyField) {
                    hasAtLeastOne = true;
                    if (item.getMedicineName() == null || item.getMedicineName().isBlank()) {
                        bindingResult.rejectValue("medicineItems[" + i + "].medicineName", "error.medicineName", "Medicine Name is required");
                    }
                    if (item.getPrice() == null) {
                        bindingResult.rejectValue("medicineItems[" + i + "].price", "error.price", "Price is required");
                    } else if (item.getPrice().compareTo(BigDecimal.ZERO) < 0) {
                        bindingResult.rejectValue("medicineItems[" + i + "].price", "error.price", "Price cannot be negative");
                    }
                    if (item.getExpiryDate() == null) {
                        bindingResult.rejectValue("medicineItems[" + i + "].expiryDate", "error.expiryDate", "Expiry Date is required");
                    }
                }
            }
            if (!hasAtLeastOne) {
                bindingResult.rejectValue("medicineItems", "error.medicineItems", "At least one medicine row is required");
            }
        } else if (dto.getDonationType() == DonationType.FOOD) {
            List<DonorFormDTO.FoodItemDTO> items = dto.getFoodItems();
            boolean hasAtLeastOne = false;
            for (int i = 0; i < items.size(); i++) {
                DonorFormDTO.FoodItemDTO item = items.get(i);
                boolean hasAnyField = (item.getFoodName() != null && !item.getFoodName().isBlank())
                        || (item.getQuantity() != null && !item.getQuantity().isBlank());
                if (hasAnyField) {
                    hasAtLeastOne = true;
                    if (item.getFoodName() == null || item.getFoodName().isBlank()) {
                        bindingResult.rejectValue("foodItems[" + i + "].foodName", "error.foodName", "Food Name is required");
                    }
                    if (item.getQuantity() == null || item.getQuantity().isBlank()) {
                        bindingResult.rejectValue("foodItems[" + i + "].quantity", "error.quantity", "Quantity is required");
                    }
                }
            }
            if (!hasAtLeastOne) {
                bindingResult.rejectValue("foodItems", "error.foodItems", "At least one food row is required");
            }
        } else if (dto.getDonationType() == DonationType.CASH || dto.getDonationType() == DonationType.UPI || dto.getDonationType() == DonationType.CHEQUE) {
            if (dto.getDonationAmount() == null) {
                bindingResult.rejectValue("donationAmount", "error.donationAmount", "Donation Amount is required");
            }
        }
    }
}
