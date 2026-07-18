package com.oldagehome.portal.foodschedule;

/**
 * Represents the type of sponsorship for a food schedule entry.
 */
public enum SponsorshipType {
    ONE_TIME("One Time"),
    ONE_DAY("1 Day"),
    FIVE_YEAR_TITHI("5 Year Tithi"),
    TODAY_SPONSOR("Today's Sponsor"),
    LUNCH_WITH_SWEET("Lunch With Sweet");

    private final String displayName;

    SponsorshipType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
