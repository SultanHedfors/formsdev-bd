package com.example.demo.util;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class TimeUtil {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    public static String formatTime(String time) {
        if (time == null || time.trim().isEmpty()) return null;

        try {
            String cleanedTime = normalizeTime(time);
            LocalTime localTime = LocalTime.parse(cleanedTime, DateTimeFormatter.ofPattern("[H:mm][HH:mm]"));
            return localTime.format(TIME_FORMATTER);
        } catch (Exception e) {
            log.error("Invalid time format: " + time);
            return null;
        }
    }

    private static String normalizeTime(String time) {
        if (time == null) return "";

        time = time.replaceAll("[^0-9:]", "").trim();

        if (time.matches("\\d{1,2}:\\d{2}")) {
            return time;
        }
        log.error("Invalid time format after cleaning: " + time);
        return "";
    }

    public static Integer calculateDuration(String startTime, String endTime) {
        if (startTime == null || endTime == null) return null;

        startTime = formatTime(startTime);
        endTime = formatTime(endTime);


        try {
            String[] startParts = startTime.split(":");
            String[] endParts = endTime.split(":");

            int startMinutes = Integer.parseInt(startParts[0]) * 60 + Integer.parseInt(startParts[1]);
            int endMinutes = Integer.parseInt(endParts[0]) * 60 + Integer.parseInt(endParts[1]);

            return endMinutes - startMinutes;
        } catch (NumberFormatException e) {
            log.error("Error parsing cleaned time: " + startTime + " - " + endTime);
            return null;
        }
    }

    // Dodaj metodę pomocniczą, jeśli jeszcze nie masz:
    public static String addOneSecondToTimeString(String time) {
        try {
            String[] parts = time.split(":");
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            int s = 0;
            if (parts.length >= 3) {
                s = Integer.parseInt(parts[2]);
            }
            s++;
            if (s >= 60) {
                s = 0;
                m++;
                if (m >= 60) {
                    m = 0;
                    h = (h + 1) % 24;
                }
            }
            return String.format("%02d:%02d:%02d", h, m, s);
        } catch (Exception ex) {
            log.error("Cannot parse time string: {}", time, ex);
            return time;
        }
    }

}
