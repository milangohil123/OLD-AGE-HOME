package com.oldagehome.portal.foodschedule;

/**
 * Represents the type of meal served in a food schedule entry.
 */
public enum MealType {
    Breakfast("Breakfast"),
    Lunch("Lunch"),
    Dinner("Dinner");

    private final String displayName;

    MealType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
