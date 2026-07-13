package com.oldagehome.portal.donor;

/**
 * Represents the category / nature of items donated.
 * Frequency (one-time, monthly, yearly) is now stored in {@link DonationFrequency}.
 */
public enum DonationType {
    FOOD,
    MEDICINE,
    CASH,
    CHEQUE,
    UPI,
    GOODS,
    OTHER
}
