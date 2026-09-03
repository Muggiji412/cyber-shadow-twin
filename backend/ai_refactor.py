import re

with open(r"D:\intelli J Project\untitled\src\ShadowTwinUI.java", "r", encoding="utf-8") as f:
    content = f.read()

# 1. Add imports
imports = """import java.util.Map;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;"""
content = content.replace("import java.util.Map;", imports)

# 2. Add AI Method
ai_method = """
    public static String evaluateThreatWithAI(String input) {
        String apiKey = config.getProperty("gemini.api.key", "YOUR_API_KEY_HERE");
        if(apiKey.equals("YOUR_API_KEY_HERE") || apiKey.isEmpty()) {
            if(input.toLowerCase().contains("hack") || input.toLowerCase().contains("sql injection") || input.toLowerCase().contains("drop table")) {
                return "{\\"risk_level\\": \\"HIGH\\", \\"classification\\": \\"Malicious Payload\\", \\"reply\\": \\"Critical threat identified. Intervention required.\\"}";
            } else {
                return "{\\"risk_level\\": \\"LOW\\", \\"classification\\": \\"Normal Chat\\", \\"reply\\": \\"System monitoring nominal.\\"}";
            }
        }
        try {
            HttpClient client = HttpClient.newHttpClient();
            String prompt = "You are a cybersecurity AI shadow twin. Analyze this user input: '" + input + "'. Return ONLY a JSON object (no markdown, no backticks) with strictly these keys: 'risk_level' (LOW, MEDIUM, HIGH), 'classification' (e.g. SQL Injection, Normal Chat, Phishing), and 'reply' (a short 1-sentence robotic response to the user).";
            String requestBody = "{\\"contents\\":[{\\"parts\\":[{\\"text\\":\\"" + prompt.replace("\\"", "\\\\\\"") + "\\"}]}]}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String res = response.body();
            int textStart = res.indexOf("\\"text\\": \\"");
            if(textStart != -1) {
                String parsed = res.substring(textStart + 9);
                parsed = parsed.substring(0, parsed.indexOf("\\""));
                parsed = parsed.replace("\\\\n", "").replace("\\\\\\"", "\\"").replace("\\\\\\\\", "\\\\").trim();
                return parsed;
            }
            return "{\\"risk_level\\": \\"MEDIUM\\", \\"classification\\": \\"Unknown\\", \\"reply\\": \\"Analysis completed with unexpected response format.\\"}";
        } catch (Exception e) {
            e.printStackTrace();
            return "{\\"risk_level\\": \\"HIGH\\", \\"classification\\": \\"AI Connection Error\\", \\"reply\\": \\"Failed to connect to AI engine.\\"}";
        }
    }

    public static String getReply"""
content = content.replace("    public static String getReply", ai_method)

# 3. Modify API simulate endpoint
old_simulate = """                    String reply = getReply(message);
                    try {
                        Connection con = getDbConnection();
                        PreparedStatement pst = con.prepareStatement("INSERT INTO chats(user_message,twin_reply) VALUES(?,?)");
                        pst.setString(1, message);
                        pst.setString(2, reply);
                        pst.executeUpdate();
                        con.close();
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                    
                    String response = "{\\"reply\\": \\"" + reply.replace("\\"", "\\\\\\"").replace("\\n", "\\\\n") + "\\"}";"""

new_simulate = """                    String aiJson = evaluateThreatWithAI(message);
                    String reply = "Analysis complete.";
                    String riskLevel = "LOW";
                    String classification = "Unknown";
                    
                    try {
                        if (aiJson.contains("\\"reply\\"")) {
                            int start = aiJson.indexOf(":", aiJson.indexOf("\\"reply\\"")) + 1;
                            while(aiJson.charAt(start) == ' ' || aiJson.charAt(start) == '"') start++;
                            int end = aiJson.indexOf("\\"", start);
                            reply = aiJson.substring(start, end);
                        }
                        if (aiJson.contains("\\"risk_level\\"")) {
                            int start = aiJson.indexOf(":", aiJson.indexOf("\\"risk_level\\"")) + 1;
                            while(aiJson.charAt(start) == ' ' || aiJson.charAt(start) == '"') start++;
                            int end = aiJson.indexOf("\\"", start);
                            riskLevel = aiJson.substring(start, end);
                        }
                        if (aiJson.contains("\\"classification\\"")) {
                            int start = aiJson.indexOf(":", aiJson.indexOf("\\"classification\\"")) + 1;
                            while(aiJson.charAt(start) == ' ' || aiJson.charAt(start) == '"') start++;
                            int end = aiJson.indexOf("\\"", start);
                            classification = aiJson.substring(start, end);
                        }
                    } catch (Exception e) { e.printStackTrace(); }

                    try {
                        Connection con = getDbConnection();
                        PreparedStatement pst = con.prepareStatement("INSERT INTO chats(user_message,twin_reply) VALUES(?,?)");
                        pst.setString(1, message);
                        pst.setString(2, reply);
                        pst.executeUpdate();
                        
                        if(riskLevel.equals("HIGH") || riskLevel.equals("MEDIUM")) {
                            PreparedStatement logPst = con.prepareStatement("INSERT INTO security_events(event_type, details) VALUES(?,?)");
                            logPst.setString(1, "AI Threat: " + classification);
                            logPst.setString(2, "Detected via input: " + message);
                            logPst.executeUpdate();
                        }
                        con.close();
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                    
                    String response = "{\\"reply\\": \\"" + reply.replace("\\"", "\\\\\\"").replace("\\n", "\\\\n") + "\\"}";"""

content = content.replace(old_simulate, new_simulate)

with open(r"D:\intelli J Project\untitled\src\ShadowTwinUI.java", "w", encoding="utf-8") as f:
    f.write(content)

print("AI Phase Complete")
