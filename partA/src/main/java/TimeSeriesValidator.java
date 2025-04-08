//חלק א סעיף ב1 א

import java.io.*;
import java.nio.file.*;
import java.text.*;
import java.util.*;

public class TimeSeriesValidator {
    private static final String DATE_FORMAT = "dd/MM/yyyy HH:mm";

    /**
     * Validates the structure and contents of a time series CSV file.
     * Checks include:
     * - Correct header
     * - Each row has two columns
     * - Timestamp is in the correct format
     * - No duplicate timestamps
     * - Value is not empty, is numeric, and is not negative
     */
    public static void validateCSV(String filePath) throws IOException {
        Set<String> timestamps = new HashSet<>(); // Used to detect duplicate timestamps
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        dateFormat.setLenient(false); // Strict parsing - invalid dates will throw ParseException

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
            String line = reader.readLine(); // Read the header line
            if (line == null || !line.equals("timestamp,value")) {
                throw new IllegalArgumentException("Error: File does not contain a valid title");
            }

            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String[] parts = line.split(",");

                // Check number of columns
                if (parts.length != 2) {
                    System.out.println("Error at line " + lineNumber + ": number of columns not valid");
                    continue;
                }

                String timestamp = parts[0].trim();
                String valueStr = parts[1].trim();

                // Validate timestamp format
                try {
                    dateFormat.parse(timestamp);
                } catch (ParseException e) {
                    System.out.println("Date format error in row " + lineNumber + ": " + timestamp);
                    continue;
                }

                // Check for duplicate timestamps
                if (!timestamps.add(timestamp)) {
                    System.out.println("Duplicate timestamp at row " + lineNumber + ": " + timestamp);
                }

                // Check if value is missing
                if (valueStr.isEmpty()) {
                    System.out.println("Value missing in row: " + lineNumber);
                    continue;
                }

                // Check if value is a valid number
                try {
                    double value = Double.parseDouble(valueStr);
                    if (value < 0) {
                        System.out.println("Negative value at row " + lineNumber + ": " + value);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Non-numeric value at row " + lineNumber + ": " + valueStr);
                }
            }
        }
    }

     public static void main(String[] args) throws IOException {
        String filePath = "time_series.csv"; // File to validate
        validateCSV(filePath);
    }
}
