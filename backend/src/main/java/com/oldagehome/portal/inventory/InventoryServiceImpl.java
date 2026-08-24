package com.oldagehome.portal.inventory;

import com.oldagehome.portal.donor.FoodDonationItem;
import com.oldagehome.portal.donor.FoodDonationItemRepository;
import com.oldagehome.portal.donor.MedicineDonationItem;
import com.oldagehome.portal.donor.MedicineDonationItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    private final FoodDonationItemRepository foodRepo;
    private final MedicineDonationItemRepository medicineRepo;

    // Pattern to extract numbers and letters (e.g., "10 KG" -> "10", "KG")
    private static final Pattern QUANTITY_PATTERN = Pattern.compile("^\\s*([0-9]+(?:\\.[0-9]+)?)\\s*([a-zA-Z]+)?.*");

    @Autowired
    public InventoryServiceImpl(FoodDonationItemRepository foodRepo, MedicineDonationItemRepository medicineRepo) {
        this.foodRepo = foodRepo;
        this.medicineRepo = medicineRepo;
    }

    @Override
    public List<InventoryItemDTO> getAggregatedInventory(String keyword, String category) {
        // Map key: "itemName_unit", Map value: InventoryItemDTO
        Map<String, InventoryItemDTO> aggregatedMap = new HashMap<>();

        boolean fetchFood = category == null || category.isEmpty() || category.equalsIgnoreCase("All") || category.equalsIgnoreCase("Food");
        boolean fetchMedicine = category == null || category.isEmpty() || category.equalsIgnoreCase("All") || category.equalsIgnoreCase("Medicine");

        if (fetchFood) {
            List<FoodDonationItem> foods = foodRepo.findAll();
            for (FoodDonationItem f : foods) {
                if (f.getDonor() == null || f.getFoodName() == null) continue;
                
                String rawName = f.getFoodName().trim();
                String rawQuantity = f.getQuantity() != null ? f.getQuantity().trim() : "1 Unit";
                
                double amount = 1.0;
                String unit = "Unit";
                
                Matcher m = QUANTITY_PATTERN.matcher(rawQuantity);
                if (m.matches()) {
                    try {
                        amount = Double.parseDouble(m.group(1));
                        if (m.group(2) != null && !m.group(2).trim().isEmpty()) {
                            unit = m.group(2).trim().toUpperCase();
                        }
                    } catch (NumberFormatException ignored) {}
                }

                String key = (rawName + "_" + unit).toLowerCase();
                
                InventoryItemDTO itemDto = aggregatedMap.computeIfAbsent(key, k -> InventoryItemDTO.builder()
                        .itemName(rawName)
                        .category("Food")
                        .totalQuantity(0.0)
                        .unit(unit)
                        .donorsCount(0)
                        .lastContributionDate(null)
                        .contributors(new ArrayList<>())
                        .build());
                        
                itemDto.setTotalQuantity(itemDto.getTotalQuantity() + amount);
                
                // Track contributor
                InventoryContributorDTO contributor = InventoryContributorDTO.builder()
                        .donorName(f.getDonor().getFullName())
                        .quantity(amount == (long)amount ? String.format("%d %s", (long)amount, unit) : String.format("%.2f %s", amount, unit))
                        .donationDate(f.getDonor().getDonationDate())
                        .donationType(f.getDonor().getDonationType().name())
                        .build();
                        
                itemDto.getContributors().add(contributor);
                
                // Update last contribution date
                if (f.getDonor().getDonationDate() != null) {
                    if (itemDto.getLastContributionDate() == null || f.getDonor().getDonationDate().isAfter(itemDto.getLastContributionDate())) {
                        itemDto.setLastContributionDate(f.getDonor().getDonationDate());
                    }
                }
            }
        }

        if (fetchMedicine) {
            List<MedicineDonationItem> meds = medicineRepo.findAll();
            for (MedicineDonationItem m : meds) {
                if (m.getDonor() == null || m.getMedicineName() == null) continue;
                
                String rawName = m.getMedicineName().trim();
                double amount = 1.0;
                String unit = "Unit";
                
                String key = (rawName + "_" + unit).toLowerCase();
                
                InventoryItemDTO itemDto = aggregatedMap.computeIfAbsent(key, k -> InventoryItemDTO.builder()
                        .itemName(rawName)
                        .category("Medicine")
                        .totalQuantity(0.0)
                        .unit(unit)
                        .donorsCount(0)
                        .lastContributionDate(null)
                        .contributors(new ArrayList<>())
                        .build());
                        
                itemDto.setTotalQuantity(itemDto.getTotalQuantity() + amount);
                
                // Track contributor
                InventoryContributorDTO contributor = InventoryContributorDTO.builder()
                        .donorName(m.getDonor().getFullName())
                        .quantity("1 Unit")
                        .donationDate(m.getDonor().getDonationDate())
                        .donationType(m.getDonor().getDonationType().name())
                        .build();
                        
                itemDto.getContributors().add(contributor);
                
                // Update last contribution date
                if (m.getDonor().getDonationDate() != null) {
                    if (itemDto.getLastContributionDate() == null || m.getDonor().getDonationDate().isAfter(itemDto.getLastContributionDate())) {
                        itemDto.setLastContributionDate(m.getDonor().getDonationDate());
                    }
                }
            }
        }

        // Post-process to count unique donors and sort contributors
        List<InventoryItemDTO> result = new ArrayList<>();
        for (InventoryItemDTO dto : aggregatedMap.values()) {
            // Count unique donors
            long uniqueDonors = dto.getContributors().stream().map(InventoryContributorDTO::getDonorName).distinct().count();
            dto.setDonorsCount((int) uniqueDonors);
            
            // Sort contributors by date descending
            dto.getContributors().sort((c1, c2) -> {
                if (c1.getDonationDate() == null && c2.getDonationDate() == null) return 0;
                if (c1.getDonationDate() == null) return 1;
                if (c2.getDonationDate() == null) return -1;
                return c2.getDonationDate().compareTo(c1.getDonationDate());
            });
            
            result.add(dto);
        }

        // Apply keyword filter if present
        if (keyword != null && !keyword.trim().isEmpty()) {
            String lowerKeyword = keyword.trim().toLowerCase();
            result = result.stream()
                    .filter(i -> i.getItemName().toLowerCase().contains(lowerKeyword) || 
                                 i.getCategory().toLowerCase().contains(lowerKeyword))
                    .collect(Collectors.toList());
        }

        // Sort overall result by itemName alphabetically
        result.sort(Comparator.comparing(InventoryItemDTO::getItemName));
        
        return result;
    }

    @Override
    public long countTotalItems() {
        return getAggregatedInventory(null, null).size();
    }

    @Override
    public long countTotalQuantity() {
        List<InventoryItemDTO> items = getAggregatedInventory(null, null);
        return (long) items.stream().mapToDouble(InventoryItemDTO::getTotalQuantity).sum();
    }

    @Override
    public long countFoodCategories() {
        List<InventoryItemDTO> items = getAggregatedInventory(null, "Food");
        return items.stream().map(InventoryItemDTO::getItemName).distinct().count();
    }

    @Override
    public long countRecentContributions() {
        // Contributions in the last 30 days
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        List<InventoryItemDTO> items = getAggregatedInventory(null, null);
        return items.stream()
                .flatMap(item -> item.getContributors().stream())
                .filter(c -> c.getDonationDate() != null && !c.getDonationDate().isBefore(thirtyDaysAgo))
                .count();
    }
}
