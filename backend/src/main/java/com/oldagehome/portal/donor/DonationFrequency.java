package com.oldagehome.portal.donor;

/**
 * Represents how often a donor makes contributions.
 * This is separate from DonationType (what is donated).
 */
public enum DonationFrequency {
    ONE_TIME("One Time"),
    MONTHLY("Monthly"),
    YEARLY("Yearly");

    private final String displayName;

    DonationFrequency(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
