// Imports for reading Parquet, handling files, dates, and data structures
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.hadoop.fs.Path;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.avro.generic.GenericRecord;

public class CSVParquetReader {

        private static final String INPUT_DATE_FORMAT = "dd/MM/yyyy HH:mm";
        private static final String OUTPUT_DATE_FORMAT = "dd/MM/yyyy HH:00";
    
        // Reads a Parquet file and returns the data as a list of [timestamp, value] arrays
        public static List<String[]> readParquet(String parquetFilePath) throws IOException {
            List<String[]> data = new ArrayList<>();
            Path path = new Path(parquetFilePath);
    
            try (ParquetReader<GenericRecord> reader = AvroParquetReader.<GenericRecord>builder(path).build()) {
                GenericRecord record;
                while ((record = reader.read()) != null) {
                    // Extract timestamp and value fields
                    String timestamp = record.get("timestamp").toString();
                    String value = record.get("value").toString();
                    data.add(new String[] { timestamp, value });
                }
            }
    
            return data;
        }

    // Calculates hourly average values from timestamped data
    public static Map<String, Double> hourlyAverages(List<String[]> dataRows) throws ParseException {
        SimpleDateFormat inputFormat = new SimpleDateFormat(INPUT_DATE_FORMAT);
        SimpleDateFormat outputFormat = new SimpleDateFormat(OUTPUT_DATE_FORMAT);
        Map<String, List<Double>> sortedHour = new TreeMap<>();

        for (String[] parts : dataRows) {
            if (parts.length != 2) continue;

            String timestampStr = parts[0].trim();
            String valueStr = parts[1].trim();

            try {
                Date timestamp = inputFormat.parse(timestampStr); // Parse date
                String hourKey = outputFormat.format(timestamp);  // Round to full hour
                double value = Double.parseDouble(valueStr);      // Parse value

                sortedHour.putIfAbsent(hourKey, new ArrayList<>());
                sortedHour.get(hourKey).add(value);
            } catch (NumberFormatException | ParseException ignored) {
                System.out.println("Skipping invalid row: " + Arrays.toString(parts));
            }
        }

        // Compute average for each hour
        Map<String, Double> hourlyAverages = new TreeMap<>();
        for (Map.Entry<String, List<Double>> entry : sortedHour.entrySet()) {
            List<Double> values = entry.getValue();
            List<Double> cleanedValues = values.stream().filter(v -> !v.isNaN()).collect(Collectors.toList());
            double average = cleanedValues.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            hourlyAverages.put(entry.getKey(), average);
        }

        return hourlyAverages;
    }

    public static void main(String[] args) {
        String csvInputFile = "time_series.csv";
        String parquetInputFile = "time_series.parquet";
        String outputFilePath = "final_averages.csv";

        try {
            File csvFile = new File(csvInputFile);
            if (csvFile.exists()) {
                // Split CSV into daily files and merge results
                partsAverageCalc.splitByDay(csvInputFile);
                File folder = new File(".");
                File[] files = folder.listFiles((dir, name) -> name.matches("day_\\d{2}_\\d{2}_\\d{4}\\.csv"));

                List<String> dailyFiles = new ArrayList<>();
                if (files != null) {
                    for (File file : files) {
                        dailyFiles.add(file.getName());
                    }
                }

                // Merge daily CSV results into final file
                partsAverageCalc.mergeResults(outputFilePath, dailyFiles);
                System.out.println("Final average calculations (from CSV) are saved in: " + outputFilePath);

            } else if (new File(parquetInputFile).exists()) {
                // Process Parquet file if CSV not found
                List<String[]> parquetData = readParquet(parquetInputFile);
                Map<String, Double> averages = hourlyAverages(parquetData);

                // Write final hourly averages to CSV
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
                    writer.write("time start\taverage");
                    writer.newLine();
                    for (Map.Entry<String, Double> entry : averages.entrySet()) {
                        writer.write(entry.getKey() + "\t" + String.format("%.2f", entry.getValue()));
                        writer.newLine();
                    }
                }

                System.out.println("Final average calculations (from Parquet) are saved in: " + outputFilePath);
            } else {
                System.out.println("No valid input file found.");
            }

        } catch (IOException | ParseException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
