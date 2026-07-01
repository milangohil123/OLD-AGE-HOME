package com.oldagehome.portal.donor;

import com.oldagehome.portal.common.AppConstants;
import com.oldagehome.portal.common.PaginationUtils;
import com.oldagehome.portal.dto.DonorImportDTO;
import com.oldagehome.portal.excel.DonorExcelExporter;
import com.oldagehome.portal.utils.FileUploadUtility;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
        model.addAttribute("donor", new Donor());
        model.addAttribute("donationTypes", DonationType.values());
        model.addAttribute("donorStatuses", DonorStatus.values());
        model.addAttribute("activePage", "donors");
        return "donors/form";
    }

    // -------------------------------------------------------------------------
    // POST /donors/save — Persist new donor
    // -------------------------------------------------------------------------

    @PostMapping("/save")
    public String saveDonor(
            @Valid @ModelAttribute("donor") Donor donor,
            BindingResult bindingResult,
            @RequestParam("photoFile") MultipartFile photoFile,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Duplicate donor ID check
        if (donor.getDonorId() != null && donorService.existsByDonorId(donor.getDonorId())) {
            bindingResult.rejectValue("donorId", "error.donor", "Donor ID already exists.");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("donationTypes", DonationType.values());
            model.addAttribute("donorStatuses", DonorStatus.values());
            model.addAttribute("activePage", "donors");
            return "donors/form";
        }

        // Handle photo upload
        try {
            if (!photoFile.isEmpty()) {
                String photoPath = fileUploadUtility.saveFile(DONORS_DIR, photoFile);
                donor.setPhoto(photoPath);
            }
        } catch (IOException e) {
            bindingResult.reject(AppConstants.Messages.ERROR_GENERIC, "Failed to upload photo file.");
            model.addAttribute("donationTypes", DonationType.values());
            model.addAttribute("donorStatuses", DonorStatus.values());
            model.addAttribute("activePage", "donors");
            return "donors/form";
        }

        donorService.saveDonor(donor);
        redirectAttributes.addFlashAttribute("successMessage", AppConstants.Messages.SUCCESS_SAVE);
        return "redirect:/donors";
    }

    // -------------------------------------------------------------------------
    // GET /donors/edit/{id} — Show pre-populated edit form
    // -------------------------------------------------------------------------

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Donor donor = donorService.getDonorById(id);
        model.addAttribute("donor", donor);
        model.addAttribute("donationTypes", DonationType.values());
        model.addAttribute("donorStatuses", DonorStatus.values());
        model.addAttribute("activePage", "donors");
        return "donors/form";
    }

    // -------------------------------------------------------------------------
    // POST /donors/update — Persist edits
    // -------------------------------------------------------------------------

    @PostMapping("/update")
    public String updateDonor(
            @Valid @ModelAttribute("donor") Donor donor,
            BindingResult bindingResult,
            @RequestParam("photoFile") MultipartFile photoFile,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Duplicate donor ID check (excluding self)
        if (donor.getId() != null && donor.getDonorId() != null) {
            Donor existing = donorService.getDonorById(donor.getId());
            if (!existing.getDonorId().equals(donor.getDonorId())
                    && donorService.existsByDonorId(donor.getDonorId())) {
                bindingResult.rejectValue("donorId", "error.donor", "Donor ID already exists.");
            }
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("donationTypes", DonationType.values());
            model.addAttribute("donorStatuses", DonorStatus.values());
            model.addAttribute("activePage", "donors");
            return "donors/form";
        }

        // Handle photo update
        try {
            if (!photoFile.isEmpty()) {
                Donor oldDonor = donorService.getDonorById(donor.getId());
                if (oldDonor.getPhoto() != null) {
                    fileUploadUtility.deleteFile(oldDonor.getPhoto());
                }
                String photoPath = fileUploadUtility.saveFile(DONORS_DIR, photoFile);
                donor.setPhoto(photoPath);
            }
        } catch (IOException e) {
            bindingResult.reject(AppConstants.Messages.ERROR_GENERIC, "Failed to upload new photo file.");
            model.addAttribute("donationTypes", DonationType.values());
            model.addAttribute("donorStatuses", DonorStatus.values());
            model.addAttribute("activePage", "donors");
            return "donors/form";
        }

        donorService.updateDonor(donor);
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
            long failCount    = results.stream().filter(r -> !r.isValid()).count();

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

    private Pageable buildDonorPageable(int page, int size, String sort, String direction) {
        String normalizedSort = sort != null ? sort.trim().toLowerCase() : "";
        String property;

        switch (normalizedSort) {
            case "name":
            case "fullname":
            case "full_name":
                property = "fullName";
                break;
            case "donorid":
            case "donor_id":
            case "idcode":
                property = "donorId";
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
}
