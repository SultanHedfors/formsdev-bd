package com.example.demo.util;

import com.example.demo.entity.WorkSchedule;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class ReportCreator {

    static void writeLogFile(String excelFilePath, List<String> logMessages, boolean success, List<WorkSchedule> workSchedules,String excelName) {
        String baseFileName = new File(excelFilePath).getName().replace(".xlsx", "");
        String fileSuffix = success ? "_processing_successful_report.txt" : "_processing_failure_report.txt";
//        String logDirectory = excelFilePath;
        String logFilePath = excelFilePath + fileSuffix;

        // Delete existing report files
        deleteOldReports(excelFilePath);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath))) {
            writer.write("Processing Report\n");
            writer.write("=============================================================\n");
            writer.write("File Processed: " + excelName + "\n\n");

            if (success) {
                writer.write("✅ Processing completed successfully!\n");
                writer.write("Total employees processed: " + workSchedules.stream()
                        .map(WorkSchedule::getEmployee)
                        .distinct()
                        .count() + "\n\n");
                writer.write("✅ Successfully added WorkSchedule entries:\n");
                for (WorkSchedule schedule : workSchedules) {
                    writer.write("  ➡️ "+schedule.toString() + "\n");
                }
            } else {
                writer.write("❌ Processing failed!\n\n");
                writer.write("Errors and logs:\n");
                for (String logMessage : logMessages) {
                    writer.write(logMessage + "\n");
                }
            }

            writer.write("\n===================================================\n");
            writer.write("End of Report\n");

            log.info("Report successfully saved to: {}", logFilePath);
        } catch (IOException e) {
            log.error("Failed to write log file: {}", e.getMessage());
        }
    }

    /**
     * Deletes all old report files ending with "_report.txt" in the given directory.
     */
    private static void deleteOldReports(String directory) {
        try (Stream<Path> files = Files.list(Paths.get(directory))) {
            files.filter(path -> path.getFileName().toString().endsWith("_report.txt"))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            log.info("Deleted old report: {}", path);
                        } catch (IOException e) {
                            log.error("Failed to delete old report {}: {}", path, e.getMessage());
                        }
                    });
        } catch (IOException e) {
            log.error("Failed to list old reports in directory {}: {}", directory, e.getMessage());
        }
    }
}
