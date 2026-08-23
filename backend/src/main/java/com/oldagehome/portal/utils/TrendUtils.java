package com.oldagehome.portal.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TrendUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Converts a list of grouping results (Date, Count/Amount) into a JSON array string
     * representing the last `days` days. Zero-fills any missing days.
     *
     * @param results List of Object array where [0] is java.sql.Date/LocalDate and [1] is Number
     * @param days Number of days to look back, including today (e.g. 7 for last 7 days)
     * @return JSON string of the array, e.g., "[0, 5, 2, 0, 0, 1, 8]"
     */
    public static String generateTrendJson(List<Object[]> results, int days) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1);

        if (results == null) return "[]";

        // Map the results by LocalDate
        Map<LocalDate, Number> dataMap = results.stream().collect(Collectors.toMap(
                row -> {
                    Object dateObj = row[0];
                    if (dateObj instanceof java.sql.Date) {
                        return ((java.sql.Date) dateObj).toLocalDate();
                    } else if (dateObj instanceof LocalDate) {
                        return (LocalDate) dateObj;
                    } else if (dateObj instanceof java.util.Date) {
                        return new java.sql.Date(((java.util.Date) dateObj).getTime()).toLocalDate();
                    } else if (dateObj != null) {
                        return LocalDate.parse(dateObj.toString());
                    }
                    return today;
                },
                row -> (Number) row[1],
                (existing, replacement) -> existing // handle duplicates if any
        ));

        List<Number> trendList = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = startDate.plusDays(i);
            Number val = dataMap.getOrDefault(d, 0);
            
            // Format BigDecimals nicely if they exist
            if (val instanceof BigDecimal) {
                trendList.add(((BigDecimal) val).setScale(2, java.math.RoundingMode.HALF_UP));
            } else {
                trendList.add(val);
            }
        }

        try {
            return objectMapper.writeValueAsString(trendList);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
