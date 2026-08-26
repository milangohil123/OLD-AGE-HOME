package com.oldagehome.portal.inventory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    @Autowired
    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public String listInventory(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "page", defaultValue = com.oldagehome.portal.common.AppConstants.Pagination.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(value = "size", defaultValue = com.oldagehome.portal.common.AppConstants.Pagination.DEFAULT_PAGE_SIZE) int size,
            Model model) {

        List<InventoryItemDTO> fullInventoryList = inventoryService.getAggregatedInventory(keyword, category);
        
        // Manual pagination
        int totalElements = fullInventoryList.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalElements);
        
        List<InventoryItemDTO> inventoryList;
        if (startIndex < totalElements) {
            inventoryList = fullInventoryList.subList(startIndex, endIndex);
        } else {
            inventoryList = java.util.Collections.emptyList();
        }

        org.springframework.data.domain.Page<InventoryItemDTO> pageObj = 
            new org.springframework.data.domain.PageImpl<>(
                inventoryList, 
                org.springframework.data.domain.PageRequest.of(page, size), 
                totalElements
            );

        model.addAttribute("inventoryList", inventoryList);
        model.addAttribute("page", pageObj);
        model.addAttribute("keyword", keyword);
        model.addAttribute("category", category);
        model.addAttribute("activePage", "inventory");
        
        java.util.LinkedHashMap<String, Object> paginationParams = new java.util.LinkedHashMap<>();
        paginationParams.put("keyword", keyword);
        paginationParams.put("category", category);
        model.addAttribute("paginationQuery", com.oldagehome.portal.common.PaginationUtils.buildQueryString(paginationParams));

        // KPI stats
        model.addAttribute("totalItems", inventoryService.countTotalItems());
        model.addAttribute("totalQuantity", inventoryService.countTotalQuantity());
        model.addAttribute("foodCategories", inventoryService.countFoodCategories());
        model.addAttribute("recentContributions", inventoryService.countRecentContributions());

        return "inventory/list";
    }
}
