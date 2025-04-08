//חלק א סעיף ב2

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.stream.Collectors;

public class partsAverageCalc {

    private static final String INPUT_DATE_FORMAT = "dd/MM/yyyy HH:mm";
    private static final String OUTPUT_DATE_FORMAT = "dd/MM/yyyy HH:00";

    // Splits the input file into daily files based on the timestamp (date only)
    public static void splitByDay(String inputFilePath) throws IOException, ParseException {
        SimpleDateFormat inputFormat = new SimpleDateFormat(INPUT_DATE_FORMAT);
        Map<String, BufferedWriter> writers = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath))) {
            String line = reader.readLine(); // Read header line
            if (line == null || !line.equals("timestamp,value")) {
                throw new IllegalArgumentException("Error: Invalid file format");
            }

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length != 2) continue;

                String timestampStr = parts[0].trim();
                Date timestamp = inputFormat.parse(timestampStr);

                // Create key by day only (no hours)
                SimpleDateFormat dayFormat = new SimpleDateFormat("dd_MM_yyyy");
                String dayKey = dayFormat.format(timestamp);

                // Get or create a writer for that day
                BufferedWriter writer = writers.computeIfAbsent(dayKey, key -> {
                    try {
                        return new BufferedWriter(new FileWriter(String.format("day_%s.csv", key)));
                    } catch (IOException e) {
                        throw new RuntimeException("Error creating daily file: " + key, e);
                    }
                });
                writer.write(line);
                writer.newLine();
            }
        }

        // Close all writers
        for (BufferedWriter writer : writers.values()) {
            writer.close();
        }
    }

    // Calculates average value for each hour from the given file
    public static Map<String, Double> hourlyAverages(String filePath) throws IOException, ParseException {
        SimpleDateFormat inputFormat = new SimpleDateFormat(INPUT_DATE_FORMAT);
        SimpleDateFormat outputFormat = new SimpleDateFormat(OUTPUT_DATE_FORMAT);
        Map<String, List<Double>> sortedHour = new TreeMap<>(); // Keeps hours sorted

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length != 2) continue;

                String timestampStr = parts[0].trim();
                String valueStr = parts[1].trim();

                try {
                    Date timestamp = inputFormat.parse(timestampStr);
                    String hourKey = outputFormat.format(timestamp); // Round to hour
                    double value = Double.parseDouble(valueStr);

                    sortedHour.putIfAbsent(hourKey, new ArrayList<>());
                    sortedHour.get(hourKey).add(value);
                } catch (NumberFormatException | ParseException ignored) {
                    System.out.println("Skipping invalid row: " + line);
                }
            }
        }

        // Compute average for each hour
        Map<String, Double> hourlyAverages = new TreeMap<>();
        for (Map.Entry<String, List<Double>> entry : sortedHour.entrySet()) {
            List<Double> values = entry.getValue();

            // Remove NaNs if exist
            List<Double> cleanedValues = values.stream()
                .filter(v -> !v.isNaN())
                .collect(Collectors.toList());

            double average = cleanedValues.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0); // If no valid values, return 0

            hourlyAverages.put(entry.getKey(), average);
        }

        return hourlyAverages;
    }

    // Merges all hourly averages from each day into a final output file
    public static void mergeResults(String outputFilePath, List<String> dailyFiles) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            writer.write("time start\taverage");
            writer.newLine();

            for (String dailyFile : dailyFiles) {
                try {
                    Map<String, Double> averages = hourlyAverages(dailyFile);
                    for (Map.Entry<String, Double> entry : averages.entrySet()) {
                        writer.write(entry.getKey() + "\t" + String.format("%.2f", entry.getValue()));
                        writer.newLine();
                    }
                } catch (ParseException e) {
                    System.out.println("Error parsing file: " + dailyFile + " - " + e.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) {
        String inputFilePath = "time_series.csv";
        String outputFilePath = "final_averages.csv";

        try {
            // Step 1: Split by day
            splitByDay(inputFilePath);

            // Step 2: Collect all daily file names
            File folder = new File(".");
            File[] files = folder.listFiles((dir, name) -> name.matches("day_\\d{2}_\\d{2}_\\d{4}\\.csv"));
            List<String> dailyFiles = new ArrayList<>();
            if (files != null) {
                for (File file : files) {
                    dailyFiles.add(file.getName());
                }
            }

            // Step 3: Merge all hourly averages to one file
            mergeResults(outputFilePath, dailyFiles);
            System.out.println("Final average calculations are saved in: " + outputFilePath);

        } catch (IOException | ParseException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
