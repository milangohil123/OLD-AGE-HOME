package com.oldagehome.portal.inventory;

import com.oldagehome.portal.common.AppConstants;
import com.oldagehome.portal.common.PaginationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.LinkedHashMap;

@Controller
@RequestMapping("/inventory/raw-materials")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class RawMaterialInventoryController {

    private final RawMaterialInventoryService rawMaterialService;

    @Autowired
    public RawMaterialInventoryController(RawMaterialInventoryService rawMaterialService) {
        this.rawMaterialService = rawMaterialService;
    }

    @GetMapping
    public String listRawMaterials(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = AppConstants.Pagination.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(value = "size", defaultValue = AppConstants.Pagination.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(value = "sort", defaultValue = "itemName") String sort,
            @RequestParam(value = "direction", defaultValue = AppConstants.Pagination.DEFAULT_SORT_DIRECTION) String direction,
            Model model) {

        Pageable pageable = PaginationUtils.buildPageable(page, size, sort, direction, "itemName");
        Page<RawMaterialInventory> rawMaterialsPage = rawMaterialService.searchInventory(keyword, pageable);

        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);
        LinkedHashMap<String, Object> paginationParams = new LinkedHashMap<>();
        paginationParams.put("keyword", keyword);
        paginationParams.put("sort", sort);
        paginationParams.put("direction", direction);
        model.addAttribute("paginationQuery", PaginationUtils.buildQueryString(paginationParams));

        model.addAttribute("rawMaterialsPage", rawMaterialsPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", rawMaterialsPage.getTotalPages());
        model.addAttribute("pageSize", size);
        model.addAttribute("totalElements", rawMaterialsPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("activePage", "raw-materials");
        model.addAttribute("pageTitle", "Raw Material Inventory");

        // KPIs
        long totalItems = rawMaterialService.countTotalRawMaterials();
        long lowStockCount = rawMaterialService.countLowStockMaterials(new BigDecimal("5.0")); // Example threshold: 5 units
        long todayUsages = rawMaterialService.countUsagesToday();

        model.addAttribute("totalItems", totalItems);
        model.addAttribute("lowStockCount", lowStockCount);
        model.addAttribute("todayUsages", todayUsages);

        return "inventory/raw-material-list";
    }
}
