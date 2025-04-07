//חלק א סעיף ב2

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.stream.Collectors;

public class partsAverageCalc {
    
    private static final String INPUT_DATE_FORMAT = "dd/MM/yyyy HH:mm";
    private static final String OUTPUT_DATE_FORMAT = "dd/MM/yyyy HH:00";
    
    //# מחלק את הקובץ לקבצים לפי יום    
    public static void splitByDay(String inputFilePath) throws IOException, ParseException {
        SimpleDateFormat inputFormat = new SimpleDateFormat(INPUT_DATE_FORMAT);
        Map<String, BufferedWriter> writers = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath))) {
            String line = reader.readLine(); // קריאת כותרת
            if (line == null || !line.equals("timestamp,value")) {
                throw new IllegalArgumentException("Error: Invalid file format");
            }

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length != 2) continue;

                String timestampStr = parts[0].trim();
                Date timestamp = inputFormat.parse(timestampStr);
                
                SimpleDateFormat dayFormat = new SimpleDateFormat("dd_MM_yyyy"); //מסירים את החלק של השעה
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
        // סגירת כל הקבצים
        for (BufferedWriter writer : writers.values()) {
            writer.close();
        }
    }
    // לכל שעה-מחשב ממוצע, מחזיר מפה של שעות עם ערך ממוצע לכל שעה
    public static Map<String, Double> hourlyAverages(String filePath) throws IOException, ParseException {
        SimpleDateFormat inputFormat = new SimpleDateFormat(INPUT_DATE_FORMAT);
        SimpleDateFormat outputFormat = new SimpleDateFormat(OUTPUT_DATE_FORMAT);
        Map<String, List<Double>> sortedHour = new TreeMap<>(); //סדר עולה

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
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
                    System.out.println("Skipping invalid row: " + line);
                }
            }
        }
        //חישוב ממצוצע לכל שעה
        Map<String, Double> hourlyAverages = new TreeMap<>(); 
        for (Map.Entry<String, List<Double>> entry : sortedHour.entrySet()) { 
            List<Double> values = entry.getValue();
            //System.out.println("Hour: " + entry.getKey() + " Values: " + values);

            List<Double> cleanedValues = values.stream()
            .filter(v -> !v.isNaN())  // מסנן החוצה את כל ערכי ה-NaN
            .collect(Collectors.toList());

            double average = cleanedValues.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0); // אם אין ערכים בכלל, מחזירים 0

            hourlyAverages.put(entry.getKey(), average);
        }

        return hourlyAverages;
    }

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
            splitByDay(inputFilePath);

            File folder = new File(".");
            File[] files = folder.listFiles((dir, name) -> name.matches("day_\\d{2}_\\d{2}_\\d{4}\\.csv"));
            List<String> dailyFiles = new ArrayList<>();
            if (files != null) {
                for (File file : files) {
                    dailyFiles.add(file.getName());
                }
            }

            mergeResults(outputFilePath, dailyFiles);
            System.out.println("Final average calculations are saved in: " + outputFilePath);

        } catch (IOException | ParseException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}

