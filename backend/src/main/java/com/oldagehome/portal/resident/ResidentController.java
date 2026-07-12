package com.oldagehome.portal.resident;

import com.oldagehome.portal.common.AppConstants;
import com.oldagehome.portal.common.PaginationUtils;
import com.oldagehome.portal.dto.ResidentImportDTO;
import com.oldagehome.portal.excel.ResidentExcelExporter;
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
@RequestMapping("/residents")
public class ResidentController {

    private final ResidentService residentService;
    private final FileUploadUtility fileUploadUtility;

    @Autowired
    public ResidentController(ResidentService residentService, FileUploadUtility fileUploadUtility) {
        this.residentService = residentService;
        this.fileUploadUtility = fileUploadUtility;
    }

    /**
     * List all residents with keyword search and pagination support.
     */
    @GetMapping
    public String listResidents(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = AppConstants.Pagination.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(value = "size", defaultValue = AppConstants.Pagination.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(value = "sort", defaultValue = AppConstants.Pagination.DEFAULT_SORT_BY) String sort,
            @RequestParam(value = "direction", defaultValue = AppConstants.Pagination.DEFAULT_SORT_DIRECTION) String direction,
            Model model) {

        Pageable pageable = buildResidentPageable(page, size, sort, direction);
        Page<Resident> residentPage = residentService.getResidents(keyword, pageable);

        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);
        LinkedHashMap<String, Object> paginationParams = new LinkedHashMap<>();
        paginationParams.put("keyword", keyword);
        paginationParams.put("sort", sort);
        paginationParams.put("direction", direction);
        model.addAttribute("paginationQuery", PaginationUtils.buildQueryString(paginationParams));

        model.addAttribute("residentsPage", residentPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", residentPage.getTotalPages());
        model.addAttribute("pageSize", size);
        model.addAttribute("totalElements", residentPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("activePage", "residents");
        
        return "residents/list";
    }

    /**
     * Show detailed resident profile card.
     */
    @GetMapping("/view/{id}")
    public String viewResident(@PathVariable("id") Long id, Model model) {
        Resident resident = residentService.getResidentById(id);
        model.addAttribute("resident", resident);
        model.addAttribute("activePage", "residents");
        return "residents/view";
    }

    /**
     * Show registration form for a new resident.
     */
    @GetMapping("/new")
    public String showNewForm(Model model) {
        model.addAttribute("resident", new Resident());
        model.addAttribute("statuses", ResidentStatus.values());
        model.addAttribute("activePage", "residents");
        return "residents/form";
    }

    /**
     * Saves a new resident. Handles validation, file upload, and duplicate key checks.
     */
    @PostMapping("/save")
    public String saveResident(
            @Valid @ModelAttribute("resident") Resident resident,
            BindingResult bindingResult,
            @RequestParam("photoFile") MultipartFile photoFile,
            Model model,
            RedirectAttributes redirectAttributes) {



        if (bindingResult.hasErrors()) {
            model.addAttribute("statuses", ResidentStatus.values());
            model.addAttribute("activePage", "residents");
            return "residents/form";
        }

        // Handle Photo upload
        try {
            if (!photoFile.isEmpty()) {
                String photoPath = fileUploadUtility.saveFile(AppConstants.Uploads.RESIDENTS_DIR, photoFile);
                resident.setPhoto(photoPath);
            }
        } catch (IOException e) {
            bindingResult.reject(AppConstants.Messages.ERROR_GENERIC, "Failed to upload photo file.");
            model.addAttribute("statuses", ResidentStatus.values());
            model.addAttribute("activePage", "residents");
            return "residents/form";
        }

        residentService.saveResident(resident);
        redirectAttributes.addFlashAttribute("successMessage", AppConstants.Messages.SUCCESS_SAVE);
        return "redirect:/residents";
    }

    /**
     * Show form to edit an existing resident record.
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Resident resident = residentService.getResidentById(id);
        model.addAttribute("resident", resident);
        model.addAttribute("statuses", ResidentStatus.values());
        model.addAttribute("activePage", "residents");
        return "residents/form";
    }

    /**
     * Save updates to a resident record.
     */
    @PostMapping("/update")
    public String updateResident(
            @Valid @ModelAttribute("resident") Resident resident,
            BindingResult bindingResult,
            @RequestParam("photoFile") MultipartFile photoFile,
            Model model,
            RedirectAttributes redirectAttributes) {



        if (bindingResult.hasErrors()) {
            model.addAttribute("statuses", ResidentStatus.values());
            model.addAttribute("activePage", "residents");
            return "residents/form";
        }

        // Handle Photo update if new photo uploaded
        try {
            if (!photoFile.isEmpty()) {
                // Delete old photo if it exists
                Resident oldResident = residentService.getResidentById(resident.getId());
                if (oldResident.getPhoto() != null) {
                    fileUploadUtility.deleteFile(oldResident.getPhoto());
                }
                
                String photoPath = fileUploadUtility.saveFile(AppConstants.Uploads.RESIDENTS_DIR, photoFile);
                resident.setPhoto(photoPath);
            }
        } catch (IOException e) {
            bindingResult.reject(AppConstants.Messages.ERROR_GENERIC, "Failed to upload new photo file.");
            model.addAttribute("statuses", ResidentStatus.values());
            model.addAttribute("activePage", "residents");
            return "residents/form";
        }

        residentService.updateResident(resident);
        redirectAttributes.addFlashAttribute("successMessage", AppConstants.Messages.SUCCESS_UPDATE);
        return "redirect:/residents";
    }

    /**
     * Delete a resident from the system.
     */
    @GetMapping("/delete/{id}")
    public String deleteResident(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Resident resident = residentService.getResidentById(id);
        
        // Delete photo from disk if present
        if (resident.getPhoto() != null) {
            fileUploadUtility.deleteFile(resident.getPhoto());
        }
        
        residentService.deleteResident(id);
        redirectAttributes.addFlashAttribute("successMessage", AppConstants.Messages.SUCCESS_DELETE);
        return "redirect:/residents";
    }

    /**
     * Download Excel sheet containing all residents database.
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportToExcel() {
        try {
            List<Resident> residents = residentService.getAllResidents();
            byte[] data = ResidentExcelExporter.exportResidents(residents);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=residents.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(data);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Import multiple resident records from uploaded Excel sheet.
     */
    @PostMapping("/import")
    public String importExcel(
            @RequestParam("file") MultipartFile file,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a valid Excel file to import.");
            return "redirect:/residents";
        }

        try {
            List<ResidentImportDTO> results = residentService.importFromExcel(file);
            
            long successCount = results.stream().filter(ResidentImportDTO::isValid).count();
            long failCount = results.stream().filter(r -> !r.isValid()).count();
            
            model.addAttribute("importResults", results);
            model.addAttribute("successCount", successCount);
            model.addAttribute("failCount", failCount);
            model.addAttribute("activePage", "residents");
            
            return "residents/import_result";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to parse Excel file: " + e.getMessage());
            return "redirect:/residents";
        }
    }

    private Pageable buildResidentPageable(int page, int size, String sort, String direction) {
        String normalizedSort = sort != null ? sort.trim().toLowerCase() : "";
        String property;
        String resolvedDirection = direction;

        switch (normalizedSort) {
            case "name":
            case "fullname":
            case "full_name":
                property = "fullName";
                break;

            case "age":
                property = "dateOfBirth";
                resolvedDirection = "desc".equalsIgnoreCase(direction) ? "asc" : "desc";
                break;
            case "dateadded":
            case "joiningdate":
                property = "joiningDate";
                break;
            default:
                property = AppConstants.Pagination.DEFAULT_SORT_BY;
                break;
        }

        return PaginationUtils.buildPageable(page, size, property, resolvedDirection, AppConstants.Pagination.DEFAULT_SORT_BY);
    }
}
