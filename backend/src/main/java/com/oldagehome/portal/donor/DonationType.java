package com.oldagehome.portal.donor;

/**
 * Represents the category / nature of items donated.
 * Frequency (one-time, monthly, yearly) is now stored in {@link DonationFrequency}.
 */
public enum DonationType {
    CASH("Cash"),
    UPI("UPI"),
    CHEQUE("Cheque"),
    FOOD("Food"),
    MEDICINE("Medicine"),
    MILK("Milk"),
    RICE("Rice"),
    WHEAT("Wheat"),
    VEGETABLES("Vegetables"),
    OIL("Oil"),
    SUGAR("Sugar"),
    DAL("Dal"),
    FRUITS("Fruits"),
    BREAKFAST_KIT("Breakfast Kit"),
    LUNCH_KIT("Lunch Kit"),
    DINNER_KIT("Dinner Kit"),
    SNACKS("Snacks"),
    OTHER_FOOD_ITEM("Other Food Item");

    private final String displayName;

    DonationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isFoodType() {
        return this != CASH && this != UPI && this != CHEQUE && this != MEDICINE;
    }
}
