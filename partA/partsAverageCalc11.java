import java.io.*;
import java.text.*;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.ParquetReader;

public class partsAverageCalc {

    private static final String INPUT_DATE_FORMAT = "dd/MM/yyyy HH:mm";
    private static final String OUTPUT_DATE_FORMAT = "dd/MM/yyyy HH:00";

    public static void splitByDay(String inputFilePath) throws IOException, ParseException {
        SimpleDateFormat inputFormat = new SimpleDateFormat(INPUT_DATE_FORMAT);
        Map<String, BufferedWriter> writers = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath))) {
            String line = reader.readLine();
            if (line == null || !line.equals("timestamp,value")) {
                throw new IllegalArgumentException("Error: Invalid file format");
            }

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length != 2) continue;

                String timestampStr = parts[0].trim();
                Date timestamp = inputFormat.parse(timestampStr);

                SimpleDateFormat dayFormat = new SimpleDateFormat("dd_MM_yyyy");
                String dayKey = dayFormat.format(timestamp);

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
        for (BufferedWriter writer : writers.values()) {
            writer.close();
        }
    }

    public static Map<String, Double> hourlyAverages(List<String[]> dataRows) throws ParseException {
        SimpleDateFormat inputFormat = new SimpleDateFormat(INPUT_DATE_FORMAT);
        SimpleDateFormat outputFormat = new SimpleDateFormat(OUTPUT_DATE_FORMAT);
        Map<String, List<Double>> sortedHour = new TreeMap<>();

        for (String[] parts : dataRows) {
            if (parts.length != 2) continue;

            String timestampStr = parts[0].trim();
            String valueStr = parts[1].trim();

            try {
                Date timestamp = inputFormat.parse(timestampStr);
                String hourKey = outputFormat.format(timestamp);
                double value = Double.parseDouble(valueStr);

                sortedHour.putIfAbsent(hourKey, new ArrayList<>());
                sortedHour.get(hourKey).add(value);
            } catch (NumberFormatException | ParseException ignored) {
                System.out.println("Skipping invalid row: " + Arrays.toString(parts));
            }
        }

        Map<String, Double> hourlyAverages = new TreeMap<>();
        for (Map.Entry<String, List<Double>> entry : sortedHour.entrySet()) {
            List<Double> values = entry.getValue();
            List<Double> cleanedValues = values.stream().filter(v -> !v.isNaN()).collect(Collectors.toList());
            double average = cleanedValues.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            hourlyAverages.put(entry.getKey(), average);
        }

        return hourlyAverages;
    }

    public static List<String[]> readParquet(String parquetFilePath) throws IOException {
        List<String[]> data = new ArrayList<>();
        Path path = new Path(parquetFilePath);

        try (ParquetReader<GenericRecord> reader = AvroParquetReader.<GenericRecord>builder(path).build()) {
            GenericRecord record;
            while ((record = reader.read()) != null) {
                String timestamp = record.get("timestamp").toString();
                String value = record.get("value").toString();
                data.add(new String[]{timestamp, value});
            }
        }

        return data;
    }

    public static void mergeResults(String outputFilePath, List<String> dailyFiles) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            writer.write("time start\taverage");
            writer.newLine();

            for (String dailyFile : dailyFiles) {
                try {
                    List<String[]> csvData = new ArrayList<>();
                    try (BufferedReader reader = new BufferedReader(new FileReader(dailyFile))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String[] parts = line.split(",");
                            if (parts.length == 2) {
                                csvData.add(parts);
                            }
                        }
                    }
                    Map<String, Double> averages = hourlyAverages(csvData);
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
        String csvInputFile = "time_series.csv";
        String parquetInputFile = "time_series.parquet";
        String outputFilePath = "final_averages.csv";

        try {
            if (new File(csvInputFile).exists()) {
                splitByDay(csvInputFile);
                File folder = new File(".");
                File[] files = folder.listFiles((dir, name) -> name.matches("day_\\d{2}_\\d{2}_\\d{4}\\.csv"));
                List<String> dailyFiles = new ArrayList<>();
                if (files != null) {
                    for (File file : files) {
                        dailyFiles.add(file.getName());
                    }
                }
                mergeResults(outputFilePath, dailyFiles);
                System.out.println("Final average calculations (from CSV) are saved in: " + outputFilePath);

            } else if (new File(parquetInputFile).exists()) {
                List<String[]> parquetData = readParquet(parquetInputFile);
                Map<String, Double> averages = hourlyAverages(parquetData);
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
