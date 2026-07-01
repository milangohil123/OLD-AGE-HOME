package com.oldagehome.portal.inventory;

import com.oldagehome.portal.common.AppConstants;
import com.oldagehome.portal.common.PaginationUtils;
import com.oldagehome.portal.dto.InventoryImportDTO;
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
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    @Autowired
    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    // -------------------------------------------------------------------------
    // GET /inventory — List with pagination and search
    // -------------------------------------------------------------------------

    @GetMapping
    public String listInventory(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = AppConstants.Pagination.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(value = "size", defaultValue = AppConstants.Pagination.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(value = "sort", defaultValue = AppConstants.Pagination.DEFAULT_SORT_BY) String sort,
            @RequestParam(value = "direction", defaultValue = AppConstants.Pagination.DEFAULT_SORT_DIRECTION) String direction,
            Model model) {

        Pageable pageable = buildInventoryPageable(page, size, sort, direction);
        Page<Inventory> inventoryPage = inventoryService.getInventory(keyword, pageable);

        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);
        LinkedHashMap<String, Object> paginationParams = new LinkedHashMap<>();
        paginationParams.put("keyword", keyword);
        paginationParams.put("sort", sort);
        paginationParams.put("direction", direction);
        model.addAttribute("paginationQuery", PaginationUtils.buildQueryString(paginationParams));

        model.addAttribute("inventoryPage", inventoryPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("activePage", "inventory");

        // Dashboard stats for list page header cards
        model.addAttribute("totalMedicines", inventoryService.countTotalMedicines());
        model.addAttribute("availableCount", inventoryService.countAvailable());
        model.addAttribute("lowStockCount", inventoryService.countLowStock());
        model.addAttribute("outOfStockCount", inventoryService.countOutOfStock());
        model.addAttribute("expiredCount", inventoryService.countExpired());
        model.addAttribute("expiringSoonCount", inventoryService.countExpiringSoon());
        model.addAttribute("todayPurchases", inventoryService.countTodayPurchases());

        return "inventory/list";
    }

    // -------------------------------------------------------------------------
    // GET /inventory/view/{id} — Medicine detail page
    // -------------------------------------------------------------------------

    @GetMapping("/view/{id}")
    public String viewInventory(@PathVariable("id") Long id, Model model) {
        Inventory item = inventoryService.getInventoryById(id);
        model.addAttribute("item", item);
        model.addAttribute("activePage", "inventory");
        return "inventory/view";
    }

    // -------------------------------------------------------------------------
    // GET /inventory/new — Show blank add medicine form
    // -------------------------------------------------------------------------

    @GetMapping("/new")
    public String showNewForm(Model model) {
        model.addAttribute("item", new Inventory());
        model.addAttribute("categories", MedicineCategory.values());
        model.addAttribute("statuses", InventoryStatus.values());
        model.addAttribute("activePage", "inventory");
        return "inventory/form";
    }

    // -------------------------------------------------------------------------
    // POST /inventory/save — Persist new medicine
    // -------------------------------------------------------------------------

    @PostMapping("/save")
    public String saveInventory(
            @Valid @ModelAttribute("item") Inventory item,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Duplicate medicine code check
        if (item.getMedicineCode() != null && inventoryService.existsByMedicineCode(item.getMedicineCode())) {
            bindingResult.rejectValue("medicineCode", "error.item", "Medicine Code already exists.");
        }

        // Date validation
        if (item.getPurchaseDate() != null && item.getExpiryDate() != null
                && item.getPurchaseDate().isAfter(item.getExpiryDate())) {
            bindingResult.rejectValue("expiryDate", "error.item", "Expiry Date cannot be before Purchase Date.");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", MedicineCategory.values());
            model.addAttribute("statuses", InventoryStatus.values());
            model.addAttribute("activePage", "inventory");
            return "inventory/form";
        }

        inventoryService.saveInventory(item);
        redirectAttributes.addFlashAttribute("successMessage", AppConstants.Messages.SUCCESS_SAVE);
        return "redirect:/inventory";
    }

    // -------------------------------------------------------------------------
    // GET /inventory/edit/{id} — Show pre-populated edit form
    // -------------------------------------------------------------------------

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Inventory item = inventoryService.getInventoryById(id);
        model.addAttribute("item", item);
        model.addAttribute("categories", MedicineCategory.values());
        model.addAttribute("statuses", InventoryStatus.values());
        model.addAttribute("activePage", "inventory");
        return "inventory/form";
    }

    // -------------------------------------------------------------------------
    // POST /inventory/update — Persist edits
    // -------------------------------------------------------------------------

    @PostMapping("/update")
    public String updateInventory(
            @Valid @ModelAttribute("item") Inventory item,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Duplicate medicine code check (excluding self)
        if (item.getId() != null && item.getMedicineCode() != null) {
            Inventory existing = inventoryService.getInventoryById(item.getId());
            if (!existing.getMedicineCode().equals(item.getMedicineCode())
                    && inventoryService.existsByMedicineCode(item.getMedicineCode())) {
                bindingResult.rejectValue("medicineCode", "error.item", "Medicine Code already exists.");
            }
        }

        // Date validation
        if (item.getPurchaseDate() != null && item.getExpiryDate() != null
                && item.getPurchaseDate().isAfter(item.getExpiryDate())) {
            bindingResult.rejectValue("expiryDate", "error.item", "Expiry Date cannot be before Purchase Date.");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", MedicineCategory.values());
            model.addAttribute("statuses", InventoryStatus.values());
            model.addAttribute("activePage", "inventory");
            return "inventory/form";
        }

        inventoryService.updateInventory(item);
        redirectAttributes.addFlashAttribute("successMessage", AppConstants.Messages.SUCCESS_UPDATE);
        return "redirect:/inventory";
    }

    // -------------------------------------------------------------------------
    // GET /inventory/delete/{id} — Delete medicine
    // -------------------------------------------------------------------------

    @GetMapping("/delete/{id}")
    public String deleteInventory(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        inventoryService.deleteInventory(id);
        redirectAttributes.addFlashAttribute("successMessage", AppConstants.Messages.SUCCESS_DELETE);
        return "redirect:/inventory";
    }

    // -------------------------------------------------------------------------
    // GET /inventory/export — Download Excel
    // -------------------------------------------------------------------------

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportToExcel() {
        try {
            byte[] data = inventoryService.exportToExcel();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventory_medicines.xlsx")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(data);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // -------------------------------------------------------------------------
    // POST /inventory/import — Upload Excel
    // -------------------------------------------------------------------------

    @PostMapping("/import")
    public String importExcel(
            @RequestParam("file") MultipartFile file,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Please select a valid Excel file to import.");
            return "redirect:/inventory";
        }

        try {
            List<InventoryImportDTO> results = inventoryService.importFromExcel(file);

            long successCount = results.stream().filter(InventoryImportDTO::isValid).count();
            long failCount    = results.stream().filter(r -> !r.isValid()).count();

            model.addAttribute("importResults", results);
            model.addAttribute("successCount", successCount);
            model.addAttribute("failCount", failCount);
            model.addAttribute("totalCount", results.size());
            model.addAttribute("activePage", "inventory");

            return "inventory/import_result";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to parse Excel file: " + e.getMessage());
            return "redirect:/inventory";
        }
    }

    private Pageable buildInventoryPageable(int page, int size, String sort, String direction) {
        String normalizedSort = sort != null ? sort.trim().toLowerCase() : "";
        String property;

        switch (normalizedSort) {
            case "name":
            case "medicinename":
            case "medicine_name":
                property = "medicineName";
                break;
            case "code":
            case "medicinecode":
            case "medicine_code":
                property = "medicineCode";
                break;
            case "category":
                property = "category";
                break;
            case "dateadded":
            case "purchasedate":
                property = "purchaseDate";
                break;
            default:
                property = AppConstants.Pagination.DEFAULT_SORT_BY;
                break;
        }

        return PaginationUtils.buildPageable(page, size, property, direction, AppConstants.Pagination.DEFAULT_SORT_BY);
    }
}
