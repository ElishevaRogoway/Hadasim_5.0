//סעיף ב1 ב

import java.io.*;
import java.text.*;
import java.util.*;

public class HourlyAverageCalculator {

    private static final String DATE_FORMAT = "dd/MM/yyyy HH:mm";
    private static final String OUTPUT_DATE_FORMAT = "yyyy/MM/dd HH:00";

    // Function to read and process the CSV file into hourly buckets
    public static Map<String, List<Double>> processCSV(String filePath) throws IOException {
        // Using TreeMap to keep hours sorted chronologically
        Map<String, List<Double>> hourSorted = new TreeMap<>();
        SimpleDateFormat inputFormat = new SimpleDateFormat(DATE_FORMAT);  
        SimpleDateFormat outputFormat = new SimpleDateFormat(OUTPUT_DATE_FORMAT);

        // Open and read the file
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine(); // Read header
            if (line == null || !line.equals("timestamp,value")) {
                throw new IllegalArgumentException("Error: File does not contain a valid title");
            }

            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String[] parts = line.split(",");

                // Skip lines that don't have exactly 2 columns
                if (parts.length != 2) continue;

                String timestampStr = parts[0].trim();
                String valueStr = parts[1].trim();

                try {
                    // Parse timestamp and round to the hour
                    Date timestamp = inputFormat.parse(timestampStr);
                    String hourKey = outputFormat.format(timestamp);

                    // Skip empty or "NaN" values
                    if (valueStr.isEmpty() || valueStr.equalsIgnoreCase("NaN")) { 
                        System.out.println("Skipping invalid value at row " + lineNumber + ": " + valueStr);
                        continue;
                    }

                    // Convert value to double and store it under its hour key
                    double value = Double.parseDouble(valueStr);
                    hourSorted.putIfAbsent(hourKey, new ArrayList<>());
                    hourSorted.get(hourKey).add(value);

                } catch (ParseException e) {
                    System.out.println("Error in row " + lineNumber + ": " + line);
                } catch (NumberFormatException e) {
                    System.out.println("Not a number value at row " + lineNumber + ": " + valueStr);
                } 
            }           
         }
      
        return hourSorted;
    }

    // Function to calculate and print the average per hour
    public static void printAverages(Map<String, List<Double>> hourSorted) {
        System.out.println("time start\taverage");
        for (Map.Entry<String, List<Double>> entry : hourSorted.entrySet()) {
            List<Double> values = entry.getValue();     

            // Calculate average, return 0 if list is empty
            double average = values.isEmpty() ? 0 : 
                             values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            System.out.println(entry.getKey() + "\t" + String.format("%.2f", average));
        }   
    }

    // Main function to run the logic
    public static void main(String[] args) {
        String filePath = "time_series.csv";
        try {
            // Process file and print results
            Map<String, List<Double>> hourSorted = processCSV(filePath);
            printAverages(hourSorted);
        } catch (IOException e) {
            System.out.println("Error in reading file: " + e.getMessage());
        }
    }
}
