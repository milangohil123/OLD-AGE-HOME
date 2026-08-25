package com.oldagehome.portal.inventory;

import com.oldagehome.portal.donor.FoodDonationItemRepository;
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
    private final RawMaterialInventoryRepository rawMaterialRepo;
    private final InventoryUsageRepository inventoryUsageRepo;

    // Pattern to extract numbers and letters (e.g., "10 KG" -> "10", "KG")
    private static final Pattern QUANTITY_PATTERN = Pattern.compile("^\\s*([0-9]+(?:\\.[0-9]+)?)\\s*([a-zA-Z]+)?.*");

    @Autowired
    public InventoryServiceImpl(FoodDonationItemRepository foodRepo, MedicineDonationItemRepository medicineRepo, 
                                RawMaterialInventoryRepository rawMaterialRepo, InventoryUsageRepository inventoryUsageRepo) {
        this.foodRepo = foodRepo;
        this.medicineRepo = medicineRepo;
        this.rawMaterialRepo = rawMaterialRepo;
        this.inventoryUsageRepo = inventoryUsageRepo;
    }

    @Override
    public List<InventoryItemDTO> getAggregatedInventory(String keyword, String category) {
        // Map key: "itemName_unit", Map value: InventoryItemDTO
        Map<String, InventoryItemDTO> aggregatedMap = new HashMap<>();

        boolean fetchFood = category == null || category.isEmpty() || category.equalsIgnoreCase("All") || category.equalsIgnoreCase("Food");
        boolean fetchMedicine = category == null || category.isEmpty() || category.equalsIgnoreCase("All") || category.equalsIgnoreCase("Medicine");

        if (fetchFood) {
            List<Object[]> foods = foodRepo.findAllProjected();
            for (Object[] f : foods) {
                if (f[0] == null) continue;
                
                String rawName = ((String) f[0]).trim();
                String rawQuantity = f[1] != null ? ((String) f[1]).trim() : "1 Unit";
                String donorName = f[2] != null ? (String) f[2] : "Unknown";
                LocalDate donationDate = f[3] != null ? (LocalDate) f[3] : null;
                String donationType = f[4] != null ? f[4].toString() : "UNKNOWN";
                
                double amount = 1.0;
                String unit = "KG";
                
                Matcher m = QUANTITY_PATTERN.matcher(rawQuantity);
                if (m.matches()) {
                    try {
                        amount = Double.parseDouble(m.group(1));
                        if (m.group(2) != null && !m.group(2).trim().isEmpty()) {
                            unit = m.group(2).trim().toUpperCase();
                        }
                    } catch (NumberFormatException ignored) {}
                } else {
                    // Try parsing just the number if no unit
                    try {
                        amount = Double.parseDouble(rawQuantity);
                    } catch (NumberFormatException ignored) {}
                }

                String key = (rawName + "_" + unit).toLowerCase();
                final String finalUnit = unit;
                
                InventoryItemDTO itemDto = aggregatedMap.computeIfAbsent(key, k -> InventoryItemDTO.builder()
                        .itemName(rawName)
                        .category("Food")
                        .totalQuantity(0.0)
                        .unit(finalUnit)
                        .donorsCount(0)
                        .lastContributionDate(null)
                        .contributors(new ArrayList<>())
                        .build());
                        
                itemDto.setTotalQuantity(itemDto.getTotalQuantity() + amount);
                
                // Track contributor
                InventoryContributorDTO contributor = InventoryContributorDTO.builder()
                        .donorName(donorName)
                        .quantity(amount == (long)amount ? String.format("%d %s", (long)amount, unit) : String.format("%.2f %s", amount, unit))
                        .donationDate(donationDate)
                        .donationType(donationType)
                        .build();
                        
                itemDto.getContributors().add(contributor);
                
                // Update last contribution date
                if (donationDate != null) {
                    if (itemDto.getLastContributionDate() == null || donationDate.isAfter(itemDto.getLastContributionDate())) {
                        itemDto.setLastContributionDate(donationDate);
                    }
                }
            }
        }

        if (fetchMedicine) {
            List<Object[]> meds = medicineRepo.findAllProjected();
            for (Object[] m : meds) {
                if (m[0] == null) continue;
                
                String rawName = ((String) m[0]).trim();
                String donorName = m[1] != null ? (String) m[1] : "Unknown";
                LocalDate donationDate = m[2] != null ? (LocalDate) m[2] : null;
                String donationType = m[3] != null ? m[3].toString() : "UNKNOWN";

                double amount = 1.0;
                String unit = "Unit";
                
                String key = (rawName + "_" + unit).toLowerCase();
                final String finalUnit = unit;
                
                InventoryItemDTO itemDto = aggregatedMap.computeIfAbsent(key, k -> InventoryItemDTO.builder()
                        .itemName(rawName)
                        .category("Medicine")
                        .totalQuantity(0.0)
                        .unit(finalUnit)
                        .donorsCount(0)
                        .lastContributionDate(null)
                        .contributors(new ArrayList<>())
                        .build());
                        
                itemDto.setTotalQuantity(itemDto.getTotalQuantity() + amount);
                
                // Track contributor
                InventoryContributorDTO contributor = InventoryContributorDTO.builder()
                        .donorName(donorName)
                        .quantity("1 Unit")
                        .donationDate(donationDate)
                        .donationType(donationType)
                        .build();
                        
                itemDto.getContributors().add(contributor);
                
                // Update last contribution date
                if (donationDate != null) {
                    if (itemDto.getLastContributionDate() == null || donationDate.isAfter(itemDto.getLastContributionDate())) {
                        itemDto.setLastContributionDate(donationDate);
                    }
                }
            }
        }

        // Post-process to count unique donors, sort contributors, and apply actual stock deductions
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
            
            // Subtract the actual used quantity from the total historic donated quantity
            Double usedQuantity = inventoryUsageRepo.sumUsedQuantityByItemName(dto.getItemName().trim());
            if (usedQuantity != null) {
                dto.setTotalQuantity(Math.max(0, dto.getTotalQuantity() - usedQuantity));
            }
            
            // Still sync the unit if it was manually updated in RawMaterialInventory
            Optional<RawMaterialInventory> trueStockOpt = rawMaterialRepo.findByItemNameIgnoreCase(dto.getItemName().trim());
            if (trueStockOpt.isPresent()) {
                dto.setUnit(trueStockOpt.get().getUnit());
            }
            
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
    
    // --------------------------------------------------------
    // Legacy Methods for ReportController compatibility
    // --------------------------------------------------------
    
    @Override
    public long countTotalMedicines() {
        return getAggregatedInventory(null, "Medicine").size();
    }

    @Override
    public long countLowStock() {
        return 0; // Dynamic aggregated inventory doesn't natively track low stock
    }

    @Override
    public long countExpired() {
        return 0; // Dynamic aggregated inventory doesn't natively track expiration yet
    }

    @Override
    public long countAvailable() {
        return getAggregatedInventory(null, null).size(); 
    }
}
