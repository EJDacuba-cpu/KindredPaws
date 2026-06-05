package com.firstapp.kidredpawpaws.utils;

import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateTimeUtils {
    private static final String TAG = "DateTimeUtils";

    /**
     * Formats an ISO datetime string into a user-friendly format: MMM dd, yyyy • hh:mm a
     * Automatically converts to the device's local timezone.
     */
    public static String formatAppointmentDateTime(String scheduledAt) {
        if (scheduledAt == null || scheduledAt.isEmpty()) return "Unknown Date";

        Log.d(TAG, "Raw scheduled_at for formatting: " + scheduledAt);

        Date date = parseIsoDateTime(scheduledAt);
        if (date == null) return scheduledAt;

        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());
        // SimpleDateFormat uses the device's default timezone.
        String formatted = formatter.format(date);
        Log.d(TAG, "Formatted date/time: " + formatted);
        return formatted;
    }

    /**
     * Parses an ISO 8601 string into a Date object.
     * Handles cases with/without offsets, fractional seconds, and space separators.
     */
    public static Date parseIsoDateTime(String isoString) {
        if (isoString == null || isoString.isEmpty()) return null;
        
        String input = isoString.replace(" ", "T");
        
        // Remove fractional seconds if present (.000) but keep timezone offset
        if (input.contains(".")) {
            int dotIndex = input.indexOf(".");
            int plusIndex = input.indexOf("+", dotIndex);
            int minusIndex = input.indexOf("-", dotIndex);
            int zIndex = input.indexOf("Z", dotIndex);
            
            if (plusIndex != -1) {
                input = input.substring(0, dotIndex) + input.substring(plusIndex);
            } else if (minusIndex != -1) {
                // Careful not to match the year separator
                input = input.substring(0, dotIndex) + input.substring(minusIndex);
            } else if (zIndex != -1) {
                input = input.substring(0, dotIndex) + "Z";
            } else {
                input = input.substring(0, dotIndex);
            }
        }

        SimpleDateFormat parser;
        if (input.contains("+") || (input.lastIndexOf("-") > 10)) { // Offset check
            parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
        } else if (input.endsWith("Z")) {
            parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            parser.setTimeZone(TimeZone.getTimeZone("UTC"));
        } else {
            parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            // If no offset provided, treat as UTC for Supabase consistency
            parser.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        try {
            return parser.parse(input);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing ISO date: " + isoString, e);
            return null;
        }
    }
}
