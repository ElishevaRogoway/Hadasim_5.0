//סעיף ב1 ב

import java.io.*;
import java.text.*;
import java.util.*;

public class HourlyAverageCalculator {

    //קביעת פורמטים לתאריכים
    private static final String DATE_FORMAT = "dd/MM/yyyy HH:mm";
    private static final String OUTPUT_DATE_FORMAT = "yyyy/MM/dd HH:00";

    //פונקציה לקריאת הקובץ ולעיבוד נתונים
    public static Map<String, List<Double>> processCSV(String filePath) throws IOException {
        Map<String, List<Double>> hourSorted = new TreeMap<>(); //יוצרים מפה הממויינת לפי שעה
        SimpleDateFormat inputFormat = new SimpleDateFormat(DATE_FORMAT);  
        SimpleDateFormat outputFormat = new SimpleDateFormat(OUTPUT_DATE_FORMAT);

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine(); // קריאת כותרת
            if (line == null || !line.equals("timestamp,value")) {
                throw new IllegalArgumentException("Error: File does not contain a valid title");
            }

            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String[] parts = line.split(",");

                if (parts.length != 2) continue; //אם השורה אינה מכילה בדיוק 2 עמודות – מדלגים עליה.

                String timestampStr = parts[0].trim();
                String valueStr = parts[1].trim();

                try {
                    Date timestamp = inputFormat.parse(timestampStr); // ממיר את המחרוזת לתאריך
                    String hourKey = outputFormat.format(timestamp); // עיגול לשעה שלמה והמרה למחרוזת
                   
                    if (valueStr.isEmpty() || valueStr.equalsIgnoreCase("NaN")) { 
                        System.out.println("Skipping invalid value at row " + lineNumber + ": " + valueStr);
                        continue;
                    }

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

    // פונקציה שמדפיסה את ממוצע הערכים לכל שעה
    public static void printAverages(Map<String, List<Double>> hourSorted) {
        System.out.println("time start\taverage");
        for (Map.Entry<String, List<Double>> entry : hourSorted.entrySet()) {
            List<Double> values = entry.getValue();     
            double average = values.isEmpty() ? 0 : values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            System.out.println(entry.getKey() + "\t" + String.format("%.2f", average));
        }   
    }

    public static void main(String[] args) {
        String filePath = "time_series.csv";
        try {
            Map<String, List<Double>> hourSorted = processCSV(filePath);
            printAverages(hourSorted);
        } catch (IOException e) {
            System.out.println("Error in reading file: " + e.getMessage());
        }
    }
}


