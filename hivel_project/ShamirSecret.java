import java.io.*;
import java.util.*;

class Point {
    int x;
    long y;
    
    Point(int x, long y) {
        this.x = x;
        this.y = y;
    }
}

public class ShamirSecret {
    
    // Decode value from given base to decimal
    public static long decodeValue(String base, String value) {
        int baseNum = Integer.parseInt(base);
        return Long.parseLong(value, baseNum);
    }
    
    // Lagrange Interpolation to find constant term c (value at x=0)
    public static long findConstantTerm(List<Point> points, int k) {
        double c = 0;
        
        // For each point, calculate its contribution to f(0)
        for (int i = 0; i < k; i++) {
            int xi = points.get(i).x;
            long yi = points.get(i).y;
            
            // Calculate Lagrange basis polynomial Li(0)
            double li = yi;
            
            for (int j = 0; j < k; j++) {
                if (i != j) {
                    int xj = points.get(j).x;
                    // Li(0) = product of (0 - xj) / (xi - xj)
                    li = li * (0 - xj) / (xi - xj);
                }
            }
            
            c += li;
        }
        
        return Math.round(c);
    }
    
    // Extract value from JSON string - handles both quoted and unquoted values
    public static String extractValue(String jsonStr, String key) {
        String search = "\"" + key + "\"";
        int keyIndex = jsonStr.indexOf(search);
        if (keyIndex == -1) return null;
        
        int colonIndex = jsonStr.indexOf(":", keyIndex);
        if (colonIndex == -1) return null;
        
        int startIndex = colonIndex + 1;
        
        // Skip whitespace
        while (startIndex < jsonStr.length() && 
               (jsonStr.charAt(startIndex) == ' ' || jsonStr.charAt(startIndex) == '\t')) {
            startIndex++;
        }
        
        // Check if value is quoted
        if (startIndex < jsonStr.length() && jsonStr.charAt(startIndex) == '"') {
            startIndex++; // Skip opening quote
            int endIndex = jsonStr.indexOf("\"", startIndex);
            if (endIndex == -1) return null;
            return jsonStr.substring(startIndex, endIndex);
        } else {
            // Unquoted value (number)
            int endIndex = startIndex;
            while (endIndex < jsonStr.length() && 
                   jsonStr.charAt(endIndex) != ',' && 
                   jsonStr.charAt(endIndex) != '}' &&
                   jsonStr.charAt(endIndex) != '\n' &&
                   jsonStr.charAt(endIndex) != '\r') {
                endIndex++;
            }
            return jsonStr.substring(startIndex, endIndex).trim();
        }
    }
    
    // Parse nested JSON object
    public static Map<String, String> parseNestedObject(String jsonStr) {
        Map<String, String> result = new HashMap<>();
        
        // Remove outer braces
        jsonStr = jsonStr.trim();
        if (jsonStr.startsWith("{")) {
            jsonStr = jsonStr.substring(1);
        }
        if (jsonStr.endsWith("}")) {
            jsonStr = jsonStr.substring(0, jsonStr.length() - 1);
        }
        
        // Simple parser for key-value pairs
        String[] parts = jsonStr.split(",");
        for (String part : parts) {
            String[] keyVal = part.split(":", 2);
            if (keyVal.length == 2) {
                String key = keyVal[0].trim().replace("\"", "");
                String value = keyVal[1].trim().replace("\"", "");
                result.put(key, value);
            }
        }
        
        return result;
    }
    
    // Main function to process JSON and find secret
    public static void findSecret(String filename) {
        try {
            // Read file
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            StringBuilder jsonContent = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
            reader.close();
            
            String content = jsonContent.toString();
            
            // Extract n and k from keys object
            int n = Integer.parseInt(extractValue(content, "n"));
            int k = Integer.parseInt(extractValue(content, "k"));
            
            System.out.println("n (total roots): " + n);
            System.out.println("k (minimum roots needed): " + k);
            System.out.println("---");
            
            // Extract points
            List<Point> points = new ArrayList<>();
            
            // Find all numeric keys (representing points)
            int depth = 0;
            StringBuilder currentObj = new StringBuilder();
            String currentKey = null;
            
            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);
                
                if (c == '"') {
                    int endQuote = content.indexOf('"', i + 1);
                    if (endQuote != -1) {
                        String key = content.substring(i + 1, endQuote);
                        
                        // Check if this is a numeric key (not "keys", "n", "k", "base", "value")
                        if (!key.equals("keys") && !key.equals("n") && !key.equals("k") && 
                            !key.equals("base") && !key.equals("value")) {
                            try {
                                int x = Integer.parseInt(key);
                                
                                // Find the object for this key
                                int objStart = content.indexOf("{", endQuote);
                                int objEnd = content.indexOf("}", objStart);
                                
                                if (objStart != -1 && objEnd != -1) {
                                    String objContent = content.substring(objStart, objEnd + 1);
                                    
                                    String base = extractValue(objContent, "base");
                                    String value = extractValue(objContent, "value");
                                    
                                    if (base != null && value != null) {
                                        long y = decodeValue(base, value);
                                        points.add(new Point(x, y));
                                        System.out.println("Point " + x + ": base=" + base + 
                                                         ", value=" + value + " => decoded=" + y);
                                    }
                                }
                            } catch (NumberFormatException e) {
                                // Not a numeric key, skip
                            }
                        }
                        i = endQuote;
                    }
                }
            }
            
            // Sort points by x value
            points.sort((a, b) -> Integer.compare(a.x, b.x));
            
            // Use only first k points
            List<Point> selectedPoints = points.subList(0, Math.min(k, points.size()));
            
            System.out.println("---");
            System.out.println("Using " + k + " points for calculation");
            
            // Find the constant term (secret)
            long secret = findConstantTerm(selectedPoints, k);
            
            System.out.println("---");
            System.out.println("Secret (constant term c): " + secret);
            System.out.println();
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        System.out.println("========== TEST CASE 1 ==========");
        findSecret("testcase1.json");
        
        System.out.println("========== TEST CASE 2 ==========");
        findSecret("testcase2.json");
    }
}