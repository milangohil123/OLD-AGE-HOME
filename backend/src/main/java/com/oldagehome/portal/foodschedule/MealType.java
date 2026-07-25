package com.oldagehome.portal.foodschedule;

public enum MealType {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner");

    private final String displayName;

    MealType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
