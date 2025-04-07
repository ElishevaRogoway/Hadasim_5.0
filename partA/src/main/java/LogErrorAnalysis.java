//חלק א סעיף א
// סהכ סיבוכיות זמן - O(NlogN)
// סהכ סיבוכיות מקום - O(N)
// אם N קטן מאוד הסיבוכיות זמן יהיה קררוב לO(N)
//פירוט ניתוח הסיבוכיות מעל כל פונקציה בהערה

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class LogErrorAnalysis {

    private static final int PART_SIZE = 1000;

    //  פיצול הקובץ לחלקים (1)
    //זמן O(N) עובר על כל השורות
    //מקום O(N) PARTֹSIZE
    public static void splitFile(String filePath) throws IOException { 
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) { //קורא שורה שורה
            int part = 0;
            String line;
            List<String> lines = new ArrayList<>(); //רשימה של שורות
            while ((line = reader.readLine()) != null) {
                lines.add(line);
                if (lines.size() >= PART_SIZE) { // מוסיף לרשימה עד שהגענו לגודל המקסימלי של  החלק
                    writePart(lines, part++);
                    lines.clear();
                }
            }
            if (!lines.isEmpty()) {
                writePart(lines, part);
            }
        }
    }

    //(למנוע קריסה של התוכנית עקב מחסור בזיכרון + מאפשר קריאה מקבילית) כתיבת כל חלק לקובץ 
    private static void writePart(List<String> lines, int part) throws IOException { 
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("part_" + part + ".txt"))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }
    
    //(2)ספירת שגיאות בקובץ
    //זמן O(N) גודל הקובץ החלקי
    //מקום O(N) מספר קודי שגיאה יחודיים
    public static Map<String, Integer> countErrorsInPart(String filePath) throws IOException { 
        Map<String, Integer> counter = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String errorCode = extractErrorCode(line); //קורא לשיטה שמחלץ את קוד השגיאה מהשורה
                if (errorCode != null) {
                    counter.put(errorCode, counter.getOrDefault(errorCode, 0) + 1); //מוסיפים אותו למפת counter (סופרים כמה פעמים כל קוד מופיע)
                }
            }
        }
        return counter;
    }
    
    //חילוץ קוד השגיאה
    private static String extractErrorCode(String line) { 
        int index = line.indexOf("Error: ");
        if (index != -1) {
            return line.substring(index + 7).trim();
        }
        return null;
    }
    
    //איחוד הספירות מכל החלקים(3)
    // זמן O(N*P)  מספר קודי שגיאה * מספר חלקים מספר קבוע
    // מקום O(N) מספר קודי שגיאה
    public static Map<String, Integer> mergeCounts(List<String> partFiles) throws IOException { 
        Map<String, Integer> totalCounter = new HashMap<>();
        for (String file : partFiles) {
            Map<String, Integer> partCounter = countErrorsInPart(file);
            for (Map.Entry<String, Integer> entry : partCounter.entrySet()) {
                totalCounter.put(entry.getKey(), totalCounter.getOrDefault(entry.getKey(), 0) + entry.getValue());
            }
        }
        return totalCounter;
    }
    
    //מציאת קודי שגיאה שכיחים ביותר(4)
    //זמן O(NlogN) מיון 
    //מקום O(N) מספר קודי שגיאה
    public static List<Map.Entry<String, Integer>> topNErrors(Map<String, Integer> errorCounts, int N) {
        return errorCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue())) // מיון מהגדול לקטן
                .limit(N) // לוקח את ה-N הראשונים
                .collect(Collectors.toList());
    }
    

    //מחיקת הקבצים שנוצרו במחשב
    private static void deleteParts() {
        File dir = new File(".");
        File[] partFiles = dir.listFiles((d, name) -> name.startsWith("part_"));
        
        if (partFiles != null) {
            for (File file : partFiles) {
                file.delete();
            }
        }
    }
    


    public static void main(String[] args) throws IOException {
        String logFile = "logs.txt";
        splitFile(logFile); // שלב 1
        
        File dir = new File(".");
        List<String> partFiles = Arrays.stream(dir.list((d, name) -> name.startsWith("part_")))
                                         .collect(Collectors.toList());
        
        Map<String, Integer> errorCounts = mergeCounts(partFiles); // שלב 2+3
       
        int N = 5; // דוגמה למציאת 3 קודי השגיאה השכיחים ביותר
        
        List<Map.Entry<String, Integer>> topErrors = topNErrors(errorCounts, N); //יוצר רשימה של הזוגות ממוינים בסדר יורד
        for (Map.Entry<String, Integer> entry : topErrors) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }

        deleteParts();

    
    }

    
}
