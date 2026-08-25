package com.oldagehome.portal.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuantityParserUtil {

    private static final Pattern QUANTITY_PATTERN = Pattern.compile("^([\\d.]+)\\s*([a-zA-Z]+)$");

    public static class ParsedQuantity {
        public BigDecimal amount;
        public String unit;

        public ParsedQuantity(BigDecimal amount, String unit) {
            this.amount = amount;
            this.unit = unit;
        }
    }

    /**
     * Parses a string like "1 kg", "500 gram", "2 ltr" into a standard format.
     * Grams are converted to KG.
     * ML are converted to LITER.
     * If no unit is found, defaults to "UNIT".
     */
    public static ParsedQuantity parseQuantity(String rawQuantity) {
        if (rawQuantity == null || rawQuantity.trim().isEmpty()) {
            return new ParsedQuantity(BigDecimal.ZERO, "UNIT");
        }

        String normalized = rawQuantity.trim().toLowerCase();
        Matcher matcher = QUANTITY_PATTERN.matcher(normalized);

        if (matcher.matches()) {
            try {
                BigDecimal amount = new BigDecimal(matcher.group(1));
                String rawUnit = matcher.group(2);
                
                return normalizeUnit(amount, rawUnit);
            } catch (NumberFormatException e) {
                return new ParsedQuantity(BigDecimal.ZERO, "UNIT");
            }
        }
        
        // Try parsing just the number if no unit
        try {
            BigDecimal amount = new BigDecimal(normalized);
            return new ParsedQuantity(amount, "UNIT");
        } catch (NumberFormatException e) {
            return new ParsedQuantity(BigDecimal.ZERO, "UNIT");
        }
    }

    private static ParsedQuantity normalizeUnit(BigDecimal amount, String rawUnit) {
        switch (rawUnit) {
            case "gram":
            case "g":
            case "grams":
            case "gm":
                return new ParsedQuantity(amount.divide(new BigDecimal("1000"), 3, RoundingMode.HALF_UP), "KG");
            case "kg":
            case "kgs":
            case "kilo":
            case "kilogram":
                return new ParsedQuantity(amount, "KG");
            case "ml":
            case "milliliter":
                return new ParsedQuantity(amount.divide(new BigDecimal("1000"), 3, RoundingMode.HALF_UP), "LITER");
            case "l":
            case "ltr":
            case "liter":
            case "liters":
                return new ParsedQuantity(amount, "LITER");
            case "pcs":
            case "piece":
            case "pieces":
            case "unit":
            case "units":
                return new ParsedQuantity(amount, "UNIT");
            default:
                // Default fallback
                return new ParsedQuantity(amount, rawUnit.toUpperCase());
        }
    }
}
