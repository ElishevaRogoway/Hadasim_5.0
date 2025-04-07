//חלק א סעיף ב1 א

import java.io.*;
import java.nio.file.*;
import java.text.*;
import java.util.*;


public class TimeSeriesValidator {
    private static final String DATE_FORMAT = "dd/MM/yyyy HH:mm";

    //פונקציה לבדיקת תקינות הקובץ
    public static void validateCSV(String filePath) throws IOException {
        Set<String> timestamps = new HashSet<>(); //מערך למניעת כפילויות של חותמות זהות
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);//מוודא שהתאריכים בפורמט הנכון
        dateFormat.setLenient(false); //  מחמירים על הפורמט כדי שגאווה לא ינסה לתקן

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
            String line = reader.readLine(); // קורא את הכותרת
            if (line == null || !line.equals("timestamp,value")) {
                throw new IllegalArgumentException("Error: File does not contain a valid title");
            }
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String[] parts = line.split(","); // מפריד לפי פסיק
                //בודק שמספר העמודות הוא 2
                if (parts.length != 2) {
                    System.out.println("Error at line " + lineNumber + ": number of colums not valid ");
                    continue;
               }
               
                //בדיקת פורמט התאריך
                String timestamp = parts[0].trim();
                String valueStr = parts[1].trim();
                try {
                    dateFormat.parse(timestamp); 
                } catch (ParseException e) {
                    System.out.println("Date format error in row " + lineNumber + ": " + timestamp);
                    continue;
                }

                //בדיקת כפילויות
                if (!timestamps.add(timestamp)) {
                    System.out.println("Duplicate timestamp at row " + lineNumber + ": " + timestamp);
                }

                // בדיקת ערכים תקינים
                if (valueStr.isEmpty()) {
                    System.out.println("Value missing in row: " + lineNumber);
                    continue;
                }

                //בדיקה אם הוזן ערך מספרי תקין
                try {
                    double value = Double.parseDouble(valueStr); //ממיר את המחרוזת למספר
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
            String filePath = "time_series.csv"; // שם הקובץ לבדיקה
            validateCSV(filePath);
        }      
   }