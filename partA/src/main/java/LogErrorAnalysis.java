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

    // (1) Splits the log file into smaller parts to reduce memory usage
    // Time: O(N), Space: O(PART_SIZE)
    public static void splitFile(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            int part = 0;
            String line;
            List<String> lines = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                lines.add(line);
                if (lines.size() >= PART_SIZE) {
                    writePart(lines, part++);
                    lines.clear();
                }
            }
            if (!lines.isEmpty()) {
                writePart(lines, part);
            }
        }
    }

    // Writes each part to a separate file for independent processing
    private static void writePart(List<String> lines, int part) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("part_" + part + ".txt"))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    // (2) Counts error codes in a single file part
    // Time: O(N), Space: O(U) where U = number of unique error codes
    public static Map<String, Integer> countErrorsInPart(String filePath) throws IOException {
        Map<String, Integer> counter = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String errorCode = extractErrorCode(line);
                if (errorCode != null) {
                    counter.put(errorCode, counter.getOrDefault(errorCode, 0) + 1);
                }
            }
        }
        return counter;
    }

    // Extracts the error code from a line (after "Error: ")
    private static String extractErrorCode(String line) {
        int index = line.indexOf("Error: ");
        if (index != -1) {
            return line.substring(index + 7).trim();
        }
        return null;
    }

    // (3) Merges error counts from all parts
    // Time: O(N*P), Space: O(U)
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

    // (4) Returns top-N most frequent error codes
    // Time: O(N log N), Space: O(N)
    public static List<Map.Entry<String, Integer>> topNErrors(Map<String, Integer> errorCounts, int N) {
        return errorCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(N)
                .collect(Collectors.toList());
    }

    // Deletes temporary part files from disk
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

        // Step 1: Split the log file into manageable parts
        splitFile(logFile);

        // Step 2+3: Count and merge error frequencies
        File dir = new File(".");
        List<String> partFiles = Arrays.stream(dir.list((d, name) -> name.startsWith("part_")))
                                       .collect(Collectors.toList());
        Map<String, Integer> errorCounts = mergeCounts(partFiles);

        // Step 4: Get and print the top-N most frequent errors
        int N = 5;
        List<Map.Entry<String, Integer>> topErrors = topNErrors(errorCounts, N);
        for (Map.Entry<String, Integer> entry : topErrors) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }

        // Clean up temporary files
        deleteParts();
    }
}
