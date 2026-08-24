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
            Model model) {

        List<InventoryItemDTO> inventoryList = inventoryService.getAggregatedInventory(keyword, category);

        model.addAttribute("inventoryList", inventoryList);
        model.addAttribute("keyword", keyword);
        model.addAttribute("category", category);
        model.addAttribute("activePage", "inventory");

        // KPI stats
        model.addAttribute("totalItems", inventoryService.countTotalItems());
        model.addAttribute("totalQuantity", inventoryService.countTotalQuantity());
        model.addAttribute("foodCategories", inventoryService.countFoodCategories());
        model.addAttribute("recentContributions", inventoryService.countRecentContributions());

        return "inventory/list";
    }
}
