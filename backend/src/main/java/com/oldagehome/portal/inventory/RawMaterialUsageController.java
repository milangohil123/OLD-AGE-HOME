package com.oldagehome.portal.inventory;

import com.oldagehome.portal.common.AppConstants;
import com.oldagehome.portal.common.PaginationUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.LinkedHashMap;

@Controller
@RequestMapping("/inventory/usage")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class RawMaterialUsageController {

    private final RawMaterialInventoryService rawMaterialService;
    private final InventoryService inventoryService;

    @Autowired
    public RawMaterialUsageController(RawMaterialInventoryService rawMaterialService, InventoryService inventoryService) {
        this.rawMaterialService = rawMaterialService;
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public String listUsages(
            @RequestParam(value = "purpose", required = false) String purpose,
            @RequestParam(value = "date", required = false) String dateStr,
            @RequestParam(value = "page", defaultValue = AppConstants.Pagination.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(value = "size", defaultValue = AppConstants.Pagination.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(value = "sort", defaultValue = "usageDate") String sort,
            @RequestParam(value = "direction", defaultValue = AppConstants.Pagination.DEFAULT_SORT_DIRECTION) String direction,
            Model model) {

        LocalDate date = null;
        if (dateStr != null && !dateStr.trim().isEmpty()) {
            try {
                date = LocalDate.parse(dateStr);
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }

        Pageable pageable = PaginationUtils.buildPageable(page, size, sort, direction, "usageDate");
        Page<InventoryUsage> usagesPage = rawMaterialService.searchUsages(purpose, date, pageable);

        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);
        LinkedHashMap<String, Object> paginationParams = new LinkedHashMap<>();
        paginationParams.put("purpose", purpose);
        paginationParams.put("date", dateStr);
        paginationParams.put("sort", sort);
        paginationParams.put("direction", direction);
        model.addAttribute("paginationQuery", PaginationUtils.buildQueryString(paginationParams));

        model.addAttribute("usagesPage", usagesPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", usagesPage.getTotalPages());
        model.addAttribute("pageSize", size);
        model.addAttribute("totalElements", usagesPage.getTotalElements());
        model.addAttribute("purpose", purpose);
        model.addAttribute("date", dateStr);
        model.addAttribute("activePage", "usage-logs");
        model.addAttribute("pageTitle", "Raw Material Usage History");

        return "inventory/raw-material-usage-list";
    }

    @GetMapping("/new")
    public String showUsageForm(Model model) {
        RawMaterialUsageFormDTO dto = new RawMaterialUsageFormDTO();
        dto.setUsageDate(LocalDate.now());
        dto.getItems().add(new RawMaterialUsageFormDTO.UsageItemDTO()); // one empty row

        model.addAttribute("usageForm", dto);
        model.addAttribute("rawMaterials", inventoryService.getAggregatedInventory(null, "Food"));
        model.addAttribute("activePage", "usage-logs");
        model.addAttribute("pageTitle", "Log Daily Usage");

        return "inventory/raw-material-usage-form";
    }

    @PostMapping("/save")
    public String saveUsage(
            @Valid @ModelAttribute("usageForm") RawMaterialUsageFormDTO dto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Remove empty rows where material is null or empty, or quantity is null
        dto.getItems().removeIf(item -> item.getRawMaterialName() == null || item.getRawMaterialName().trim().isEmpty() || item.getQuantityUsed() == null);

        if (dto.getItems().isEmpty()) {
            bindingResult.rejectValue("items", "error.items", "At least one raw material must be logged.");
        }

        if (bindingResult.hasErrors()) {
            if (dto.getItems().isEmpty()) {
                dto.getItems().add(new RawMaterialUsageFormDTO.UsageItemDTO());
            }
            model.addAttribute("rawMaterials", inventoryService.getAggregatedInventory(null, "Food"));
            model.addAttribute("activePage", "usage-logs");
            model.addAttribute("pageTitle", "Log Daily Usage");
            return "inventory/raw-material-usage-form";
        }

        rawMaterialService.logUsage(dto.getUsageDate(), dto.getPurpose(), dto.getItems());
        
        redirectAttributes.addFlashAttribute("successMessage", "Usage logged successfully. Inventory stocks have been updated.");
        return "redirect:/inventory";
    }
}
