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
    MEDICINE("Medicine");

    private final String displayName;

    DonationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
