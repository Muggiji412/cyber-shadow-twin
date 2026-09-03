import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.awt.Font;
import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

public class ShadowTwinUI {

    static java.util.HashMap<String, String> sessionTokens = new java.util.HashMap<>();
    static Set<String> blockedIPs = Collections.synchronizedSet(new HashSet<>());
    static List<OutputStream> sseClients = new CopyOnWriteArrayList<>();

    public static String getClientIP(HttpExchange exchange) {
        String xff = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.trim().isEmpty()) return xff.split(",")[0].trim();
        String xri = exchange.getRequestHeaders().getFirst("X-Real-IP");
        if (xri != null && !xri.trim().isEmpty()) return xri.trim();
        String ip = exchange.getRemoteAddress().getAddress().getHostAddress();
        return ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) ? "127.0.0.1" : ip;
    }

    public static java.util.Properties config = new java.util.Properties();
    static {
        try {
            java.io.FileInputStream fis = new java.io.FileInputStream("config.properties");
            config.load(fis);
            fis.close();
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println("Could not load config.properties. Using default settings.");
        }
    }
    
    public static Connection getDbConnection() throws SQLException {
        return DriverManager.getConnection(
            config.getProperty("db.url", "jdbc:mysql://localhost:3306/shadow_twin_db"),
            config.getProperty("db.user", "root"),
            config.getProperty("db.password", "Muggi@123")
        );
    }


    static String currentOTP="";
    static String otpUser="";


    public static String evaluateThreatWithAI(String input) {
        String apiKey = config.getProperty("gemini.api.key", "YOUR_API_KEY_HERE");
        if (!apiKey.equals("YOUR_API_KEY_HERE") && !apiKey.isEmpty()) {
            try {
                HttpClient client = HttpClient.newHttpClient();
                String prompt = "You are a cybersecurity AI shadow twin. Analyze input: '" + input + "'. Return ONLY JSON: {\"risk_level\": \"LOW\"|\"MEDIUM\"|\"HIGH\", \"classification\": \"...\", \"reply\": \"...\"}";
                String requestBody = "{\"contents\":[{\"parts\":[{\"text\":\"" + prompt.replace("\"", "\\\"") + "\"}]}]}";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                String res = response.body();
                int textIdx = res.indexOf("\"text\"");
                if (textIdx != -1) {
                    int contentStart = res.indexOf("\"", res.indexOf(":", textIdx) + 1);
                    if (contentStart != -1) {
                        String rawText = res.substring(contentStart + 1, res.lastIndexOf("\""));
                        String cleaned = rawText.replace("\\n", " ").replace("```json", "").replace("```", "").replace("\\\"", "\"").replace("\\\\", "\\").trim();
                        if (cleaned.contains("risk_level")) return cleaned;
                    }
                }
            } catch (Exception e) {}
        }
        return evaluateThreatHeuristic(input);
    }

    private static String evaluateThreatHeuristic(String msg) {
        String s = msg.toLowerCase();
        if (s.contains("trojan") || s.contains("hack") || s.contains("sql") || s.contains("ddos") || s.contains("malware") || s.contains("virus") || s.contains("exploit") || s.contains("attack"))
            return "{\"risk_level\":\"HIGH\",\"classification\":\"Malicious Payload\",\"reply\":\"Cybersecurity threat detected: " + msg.replace("\"", "'") + ". Threat vector flagged and logged.\"}";
        if (s.contains("login") || s.contains("auth") || s.contains("fail") || s.contains("scan") || s.contains("port"))
            return "{\"risk_level\":\"MEDIUM\",\"classification\":\"Suspicious Activity\",\"reply\":\"Suspicious event pattern identified: " + msg.replace("\"", "'") + ".\"}";
        return "{\"risk_level\":\"LOW\",\"classification\":\"Normal Chat\",\"reply\":\"System nominal. Cyber Shadow Twin monitoring active for: " + msg.replace("\"", "'") + ".\"}";
    }

    public static String getReply(String message){

        message = message.toLowerCase();

        try{

            Connection con =
                    getDbConnection();

            Statement st =
                    con.createStatement();


            // HELLO + NAME
            if(message.contains("hello")
                    || message.contains("hi")){

                ResultSet rs =
                        st.executeQuery(
                                "SELECT memory_text FROM memories WHERE memory_text LIKE 'my name is%'"
                        );

                if(rs.next()){

                    String name =
                            rs.getString("memory_text");

                    name =
                            name.replace(
                                    "my name is ",
                                    ""
                            );

                    con.close();

                    return "Hello " + name + " 😄";
                }

                con.close();

                return "Hello 😄";
            }


            // RECALL FAVOURITE (MUST COME FIRST)
            else if(message.contains("what is my favourite")){

                String category =
                        message.replace(
                                "what is my favourite ",
                                ""
                        );

                ResultSet rs =
                        st.executeQuery(
                                "SELECT value_text FROM preferences WHERE category='"
                                        +category+
                                        "'"
                        );

                if(rs.next()){

                    String value =
                            rs.getString(
                                    "value_text"
                            );

                    con.close();

                    return
                            "Your favourite "
                                    +category+
                                    " is "
                                    +value;
                }

                con.close();

                return
                        "I don't know that yet.";
            }


            // SAVE FAVOURITE (AFTER RECALL)
            else if(message.contains("my favourite")){

                String data =
                        message.replace(
                                "my favourite ",
                                ""
                        );

                String parts[] =
                        data.split(
                                " is "
                        );

                if(parts.length==2){

                    String category =
                            parts[0];

                    String value =
                            parts[1];

                    PreparedStatement pst =
                            con.prepareStatement(
                                    "REPLACE INTO preferences(category,value_text) VALUES(?,?)"
                            );

                    pst.setString(
                            1,
                            category
                    );

                    pst.setString(
                            2,
                            value
                    );

                    pst.executeUpdate();

                    con.close();

                    return
                            "Got it. I'll remember your favourite "
                                    +category;
                }

                con.close();

                return
                        "Try: my favourite food is pizza";
            }


            // SAVE LIKES
            else if(message.contains("i like")){

                PreparedStatement pst =
                        con.prepareStatement(
                                "INSERT INTO memories(memory_text) VALUES(?)"
                        );

                pst.setString(
                        1,
                        message
                );

                pst.executeUpdate();

                con.close();

                return
                        "Got it. I'll remember that.";
            }


            // RECALL LIKES
            else if(message.contains("what do i like")){

                ResultSet rs =
                        st.executeQuery(
                                "SELECT memory_text FROM memories WHERE memory_text LIKE '%i like%'"
                        );

                String reply =
                        "You told me:\n";

                while(rs.next()){

                    reply +=
                            "- "
                                    +rs.getString("memory_text")
                                    +"\n";
                }

                con.close();

                return reply;
            }


            // PROFILE
            else if(message.contains(
                    "what do you know about me")){

                String reply =
                        "Here's what I know:\n";

                ResultSet rs1 =
                        st.executeQuery(
                                "SELECT memory_text FROM memories"
                        );

                while(rs1.next()){

                    reply +=
                            "- "
                                    +rs1.getString(
                                    "memory_text"
                            )
                                    +"\n";
                }

                ResultSet rs2 =
                        st.executeQuery(
                                "SELECT category,value_text FROM preferences"
                        );

                while(rs2.next()){

                    reply +=
                            "- Favourite "
                                    +rs2.getString(
                                    "category"
                            )
                                    +" : "
                                    +rs2.getString(
                                    "value_text"
                            )
                                    +"\n";
                }

                con.close();

                return reply;
            }
            else if(message.contains("password")){

                String password="";

                if(message.contains("is")){

                    password=
                            message.substring(
                                    message.indexOf("is")+2
                            ).trim();
                }

                // Too short
                if(password.length()<8){
                    logSecurityEvent(
                            "Password Alert",
                            "Password too short"
                    );
                    return "⚠ Security Alert: Password too short.";
                }

                // Common weak passwords
                String commonPasswords[]={

                        "password",
                        "admin",
                        "qwerty",
                        "welcome",
                        "letmein",
                        "123456",
                        "12345678",
                        "abc123"
                };

                for(String weak:commonPasswords){

                    if(password.equalsIgnoreCase(weak)){

                        logSecurityEvent(
                                "Password Alert",
                                "Common password"
                        );
                        return "⚠ Security Alert: Common password detected.";
                    }
                }

                // Numeric only
                if(password.matches("\\d+")){

                    logSecurityEvent(
                            "Password Alert",
                            "Numeric-only password"
                    );
                    return "⚠ Security Alert: Numeric-only passwords are weak.";
                }

                // Sequential numbers
                if(password.contains("123")
                        ||password.contains("456")
                        ||password.contains("789")){

                    return "⚠ Security Alert: Sequential patterns detected.";
                }

                // Repeating characters
                if(password.matches("(.)\\1{3,}.*")){

                    return "⚠ Security Alert: Too many repeated characters.";
                }


                boolean hasUpper=
                        password.matches(".*[A-Z].*");

                boolean hasLower=
                        password.matches(".*[a-z].*");

                boolean hasDigit=
                        password.matches(".*\\d.*");

                boolean hasSpecial=
                        password.matches(".*[^a-zA-Z0-9].*");


                int score=0;

                if(hasUpper)
                    score+=1;

                if(hasLower)
                    score+=1;

                if(hasDigit)
                    score+=1;

                if(hasSpecial)
                    score+=1;

                if(password.length()>=12)
                    score+=2;

                if(password.length()>=16)
                    score+=1;


                if(score<=2){

                    logSecurityEvent(
                            "Password Alert",
                            "Weak password"
                    );

                    return "⚠ Weak password.";

                }

                else if(score<=5){

                    logSecurityEvent(
                            "Password Alert",
                            "Medium password"
                    );

                    return "🟡 Medium password.";

                }

                else{

                    logSecurityEvent(
                            "Password Alert",
                            "Strong password"
                    );

                    return "✅ Strong password.";

                }

            }

            else if(

                    message.contains("hack")
                            ||message.contains("bypass")
                            ||message.contains("crack")
                            ||message.contains("steal password")
                            ||message.contains("ddos")
                            ||message.contains("sql injection")
                            ||message.contains("malware")

            ){

                logSecurityEvent(
                        "Threat Alert",
                        "Suspicious activity"
                );

                if(message.contains("sql injection")){

                    return "🚨 Threat Type: SQL Injection Attack Detected";
                }

                else if(message.contains("ddos")){

                    return "🚨 Threat Type: DDoS Activity Detected";
                }

                else if(message.contains("malware")){

                    return "🚨 Threat Type: Malware Activity Detected";
                }

                else if(message.contains("steal password")){

                    return "🚨 Threat Type: Credential Theft Attempt";
                }

                return
                        "⚠ Cyber Alert: Suspicious security-related activity detected.";
            }

            else if(message.equals("ip") ||message.contains("login ip")){

                logSecurityEvent(
                        "IP Alert",
                        "Unknown IP activity"
                );

                return
                        "🌍 Security Alert: Login attempt detected from IP: 192.168.45.23";
            }

            else if(

                    message.contains("midnight")
                            ||message.contains("2am")
                            ||message.contains("3am")
                            ||message.contains("late night")

            ){

                logSecurityEvent(
                        "Behavior Alert",
                        "Suspicious login time"
                );

                return
                        "🕒 Behavioral Alert: Unusual login time detected.";
            }

            else if(

                    message.contains("india")
                            ||message.contains("russia")
                            ||message.contains("china")
                            ||message.contains("usa")
                            ||message.contains("foreign login")

            ){

                logSecurityEvent(
                        "Location Alert",
                        "Unusual login location"
                );

                if(message.contains("india")){

                    return "📍 Login detected from India";
                }

                else if(message.contains("russia")){

                    return "🚨 Login detected from Russia — unusual location activity.";
                }

                else if(message.contains("china")){

                    return "🚨 Login detected from China — unusual location activity.";
                }

                else if(message.contains("usa")){

                    return "📍 Login detected from USA";
                }

                return "⚠ Unknown location activity detected.";
            }

            else if(message.contains("failed login")){

                logSecurityEvent(
                        "Failed Login",
                        "Failed login attempt"
                );

                int failedCount=0;

                try{

                    ResultSet rs=
                            st.executeQuery(
                                    "SELECT COUNT(*) AS total FROM security_events WHERE details='Failed login attempt'"
                            );

                    if(rs.next()){

                        failedCount=
                                rs.getInt("total");
                    }

                }

                catch(Exception e){

                    System.out.println(e);

                }

                if(failedCount>=3){

                    return
                            "🔒 Account temporarily locked due to multiple failed login attempts.";
                }

                return
                        "❌ Failed login attempt recorded.";
            }

            else if(message.equals("security tips")){

                return
                        "🛡 Security Recommendations:\n\n"

                                +"• Use strong passwords\n"

                                +"• Enable two-factor authentication\n"

                                +"• Avoid common passwords\n"

                                +"• Keep software updated\n"

                                +"• Avoid suspicious links\n"

                                +"• Monitor login activity";
            }

            else if(
                    message.equals("session")
                            ||message.contains("active session")
            ){

                logSecurityEvent(
                        "Session Activity",
                        "Session monitored"
                );

                return
                        "🖥 Active Session Information:\n\n"

                                +"Session ID: ST-2026-4582\n"

                                +"Status: Active\n"

                                +"Device: Windows Desktop\n"

                                +"Browser: Chrome\n"

                                +"Duration: 23 minutes";
            }

            else if(
                    message.equals("report")
                            ||message.contains("security report")
            ){

                int eventCount=0;

                try{

                    ResultSet rs=
                            st.executeQuery(
                                    "SELECT COUNT(*) AS total FROM security_events"
                            );

                    if(rs.next()){

                        eventCount=
                                rs.getInt("total");
                    }

                }

                catch(Exception e){

                    System.out.println(e);
                }

                return
                        "📋 Security Report\n\n"

                                +"Security Events: "
                                +eventCount
                                +"\n"

                                +"Risk Status: Active Monitoring\n"

                                +"System Status: Protected";
            }

            else if(
                    message.equals("audit")
                            ||message.contains("audit log")
            ){

                String report=
                        "📜 Security Audit Log:\n\n";

                try{

                    ResultSet rs=
                            st.executeQuery(

                                    "SELECT event_type,details,event_time FROM security_events ORDER BY id DESC LIMIT 5"

                            );

                    while(rs.next()){

                        report +=
                                rs.getString("event_type")
                                        +" | "

                                        +rs.getString("details")
                                        +" | "

                                        +rs.getString("event_time")
                                        +"\n";
                    }

                }

                catch(Exception e){

                    System.out.println(e);
                }

                return report;
            }

            else if(
                    message.equals("threat scan")
                            ||message.contains("scan")
            ){

                logSecurityEvent(
                        "Threat Scan",
                        "Threat intelligence check"
                );

                return
                        "🔍 Threat Intelligence Scan\n\n"

                                +"Status: Scan Complete\n"

                                +"Malware: Not detected\n"

                                +"Suspicious IPs: 0\n"

                                +"Known Threat Signatures: 0\n"

                                +"Recommendation: Continue monitoring";
            }

            else if(
                    message.equals("incident response")
                            ||message.contains("response plan")
            ){

                return
                        "🚑 Incident Response Plan\n\n"

                                +"1. Identify suspicious activity\n"

                                +"2. Isolate affected system\n"

                                +"3. Analyze security logs\n"

                                +"4. Remove threat source\n"

                                +"5. Recover system state\n"

                                +"6. Continue monitoring";
            }

            else if(
                    message.equals("notifications")
                            ||message.contains("alerts")
            ){

                String alerts=
                        "🔔 Recent Alerts:\n\n";

                try{

                    ResultSet rs=
                            st.executeQuery(

                                    "SELECT details,event_time FROM security_events ORDER BY id DESC LIMIT 5"

                            );

                    while(rs.next()){

                        alerts+=

                                "• "
                                        +rs.getString("details")

                                        +" ("

                                        +rs.getString("event_time")

                                        +")\n";
                    }

                }

                catch(Exception e){

                    System.out.println(e);
                }

                return alerts;
            }

            else if(message.startsWith("register")){

                String data=
                        message.replace(
                                "register",
                                ""
                        ).trim();

                String parts[]=
                        data.split(" ");

                if(parts.length>=2){

                    try{

                        PreparedStatement pst=
                                con.prepareStatement(

                                        "INSERT INTO users(username,password,phone) VALUES(?,?,?)"

                                );

                        pst.setString(1, parts[0]);

                        pst.setString(2, parts[1]);

                        if(parts.length>=3){

                            pst.setString(3, parts[2]);
                        }

                        else{

                            pst.setString(3, null);
                        }

                        pst.executeUpdate();

                        return
                                "✅ User registered successfully.";

                    }

                    catch(Exception e){

                        return
                                "ERROR: "+e;
                    }

                }

                return
                        "Use: register username password [phone optional]";
            }

            else if(message.startsWith("login")){

                String data=
                        message.replace(
                                "login",
                                ""
                        ).trim();

                String parts[]=
                        data.split(" ");

                if(parts.length>=2){

                    try{

                        PreparedStatement pst=
                                con.prepareStatement(

                                        "SELECT * FROM users WHERE username=? AND password=?"

                                );

                        pst.setString(
                                1,
                                parts[0]
                        );

                        pst.setString(
                                2,
                                parts[1]
                        );

                        ResultSet rs=
                                pst.executeQuery();

                        if(rs.next()){

                            boolean locked=
                                    rs.getBoolean("locked");

                            if(locked){

                                ResultSet rsTime=
                                        st.executeQuery(

                                                "SELECT TIMESTAMPDIFF(SECOND,lock_time,NOW()) AS secondsPassed FROM users WHERE username='"
                                                        +parts[0]
                                                        +"'"

                                        );

                                if(rsTime.next()){

                                    int seconds=
                                            rsTime.getInt(
                                                    "secondsPassed"
                                            );

                                    if(seconds>=120){

                                        st.executeUpdate(

                                                "UPDATE users SET locked=false, lock_time=NULL WHERE username='"
                                                        +parts[0]
                                                        +"'"

                                        );

                                    }

                                    else{

                                        ResultSet rsPhone=
                                                st.executeQuery(

                                                        "SELECT phone FROM users WHERE username='"
                                                                +parts[0]
                                                                +"'"

                                                );

                                        if(rsPhone.next()){

                                            String phone=
                                                    rsPhone.getString(
                                                            "phone"
                                                    );

                                            if(phone!=null
                                                    && !phone.equals("")){

                                                currentOTP=
                                                        String.valueOf(

                                                                (int)(
                                                                        1000+
                                                                                Math.random()*9000
                                                                )
                                                        );

                                                otpUser=
                                                        parts[0];

                                                st.executeUpdate(

                                                        "UPDATE users SET otp='"
                                                                +currentOTP+
                                                                "',otp_time=NOW() WHERE username='"
                                                                +parts[0]
                                                                +"'"

                                                );

                                                return
                                                        "📱 OTP generated for "
                                                                +phone
                                                                +"\nOTP: "
                                                                +currentOTP;
                                            }
                                        }

                                        return
                                                "🔒 Account locked. Try again in "
                                                        +(120-seconds)
                                                        +" seconds.";
                                    }
                                }
                            }

                            logSecurityEvent(
                                    "Login",
                                    "Successful login"
                            );

                            return
                                    "✅ Login successful.";
                        }

                        else{

                            logSecurityEvent(
                                    "Failed Login",
                                    "Invalid credentials"
                            );

                            int failedCount=0;

                            ResultSet rs2=
                                    st.executeQuery(

                                            "SELECT COUNT(*) AS total FROM security_events WHERE event_type='Failed Login'"

                                    );

                            if(rs2.next()){

                                failedCount=
                                        rs2.getInt("total");
                            }

                            if(failedCount>=3){

                                st.executeUpdate(

                                        "UPDATE users SET locked=true, lock_time=NOW() WHERE username='"
                                                +parts[0]
                                                +"'"

                                );

                                return
                                        "🔒 Account locked for 2 minutes due to multiple failed attempts.";
                            }

                            return
                                    "❌ Invalid username or password.";
                        }

                    }

                    catch(Exception e){

                        return "ERROR: "+e;
                    }

                }

                return
                        "Use: login username password";
            }
            else if(message.startsWith("otp")){

                String enteredOTP=
                        message.replace(
                                "otp",
                                ""
                        ).trim();

                if(enteredOTP.equals(currentOTP)){

                    try{

                        ResultSet rsTime=
                                st.executeQuery(

                                        "SELECT TIMESTAMPDIFF(SECOND,otp_time,NOW()) AS secondsPassed FROM users WHERE username='"
                                                +otpUser
                                                +"'"

                                );

                        if(rsTime.next()){

                            int seconds=
                                    rsTime.getInt(
                                            "secondsPassed"
                                    );

                            if(seconds>60){

                                currentOTP="";
                                otpUser="";

                                return
                                        "⌛ OTP expired. Generate a new OTP.";
                            }
                        }

                        st.executeUpdate(

                                "UPDATE users SET locked=false,lock_time=NULL,otp=NULL,otp_time=NULL WHERE username='"
                                        +otpUser
                                        +"'"

                        );

                    }

                    catch(Exception e){

                        return "ERROR: "+e;
                    }

                    currentOTP="";
                    otpUser="";

                    return
                            "✅ OTP verified. Account unlocked.";
                }

                return
                        "❌ Invalid OTP.";
            }

            else if(message.equals("reset")){

                try{

                    st.executeUpdate(
                            "DELETE FROM security_events"
                    );

                    return
                            "✅ Security events reset successfully.";
                }

                catch(Exception e){

                    return "ERROR: "+e;
                }
            }

            return "Interesting... tell me more.";

        }

        catch(Exception e){

            return "ERROR: "+e;
        }
    }




    public static String validateToken(HttpExchange exchange) {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            return sessionTokens.get(token);
        }
        return null;
    }

    public static void broadcast(String json) {
        String data = "data: " + json + "\n\n";
        byte[] bytes = data.getBytes();
        for (OutputStream os : sseClients) {
            try {
                os.write(bytes);
                os.flush();
            } catch (IOException e) {
                sseClients.remove(os);
            }
        }
    }

    public static String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) != -1) {
            bos.write(buffer, 0, len);
        }
        return bos.toString("UTF-8");
    }

    public static void startApiServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            // Ensure admin user exists
            try (Connection con = getDbConnection(); Statement st = con.createStatement()) {
                ResultSet rs = st.executeQuery("SELECT * FROM users WHERE username='admin'");
                if (!rs.next()) {
                    st.executeUpdate("INSERT INTO users (username, password, phone) VALUES ('admin', 'cst123', '555-000')");
                    System.out.println("Admin user auto-created.");
                }
            } catch (Exception e) {
                System.out.println("Could not auto-create admin user: " + e.getMessage());
            }

            
            // 1. Authenticate
            server.createContext("/api/authenticate", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                    
                    if ("OPTIONS".equals(exchange.getRequestMethod())) {
                        exchange.sendResponseHeaders(204, -1);
                        return;
                    }
                    
                    String body = readRequestBody(exchange);
                    String username = "";
                    String password = "";
                    
                    // Simple JSON parse (assuming {"username":"...", "password":"..."})
                    if (body.contains("\"username\"")) {
                        int start = body.indexOf("\"username\"") + 12;
                        while(body.charAt(start) == ' ' || body.charAt(start) == ':' || body.charAt(start) == '"') start++;
                        int end = body.indexOf("\"", start);
                        username = body.substring(start, end);
                    }
                    if (body.contains("\"password\"")) {
                        int start = body.indexOf("\"password\"") + 12;
                        while(body.charAt(start) == ' ' || body.charAt(start) == ':' || body.charAt(start) == '"') start++;
                        int end = body.indexOf("\"", start);
                        password = body.substring(start, end);
                    }
                    
                    boolean success = false;
                    try (Connection con = getDbConnection(); 
                         PreparedStatement pst = con.prepareStatement("SELECT * FROM users WHERE username=? AND password=?")) {
                        pst.setString(1, username);
                        pst.setString(2, password);
                        ResultSet rs = pst.executeQuery();
                        if (rs.next()) {
                            success = true;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    
                    String response;
                    if (success) {
                        String token = UUID.randomUUID().toString();
                        sessionTokens.put(token, username);
                        response = "{\"success\":true, \"token\":\"" + token + "\"}";
                    } else {
                        response = "{\"success\":false, \"error\":\"Invalid credentials\"}";
                    }
                    
                    byte[] responseBytes = response.getBytes();
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, responseBytes.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(responseBytes);
                    os.close();
                }
            });

            // 2. Logout
            server.createContext("/api/logout", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Authorization");
                    
                    if ("OPTIONS".equals(exchange.getRequestMethod())) {
                        exchange.sendResponseHeaders(204, -1);
                        return;
                    }
                    
                    String auth = exchange.getRequestHeaders().getFirst("Authorization");
                    if (auth != null && auth.startsWith("Bearer ")) {
                        String token = auth.substring(7);
                        sessionTokens.remove(token);
                    }
                    
                    String response = "{\"success\":true}";
                    byte[] responseBytes = response.getBytes();
                    exchange.sendResponseHeaders(200, responseBytes.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(responseBytes);
                    os.close();
                }
            });

            // 3. SSE Stream
            server.createContext("/api/events-stream", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    String query = exchange.getRequestURI().getQuery();
                    if (query == null || !query.contains("token=")) {
                        exchange.sendResponseHeaders(401, -1);
                        return;
                    }
                    
                    exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
                    exchange.getResponseHeaders().set("Cache-Control", "no-cache");
                    exchange.getResponseHeaders().set("Connection", "keep-alive");
                    exchange.sendResponseHeaders(200, 0);
                    
                    OutputStream os = exchange.getResponseBody();
                    sseClients.add(os);
                    // keep connection open
                }
            });

            // 4. Honeypot traps
            HttpHandler honeypotHandler = exchange -> {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "*");
                if ("OPTIONS".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(204, -1); return; }
                String ip = getClientIP(exchange);
                blockedIPs.add(ip);
                logSecurityEvent("HONEYPOT BREACH", "Intruder IP: " + ip);
                broadcast("{\"type\":\"honeypot\", \"ip\":\"" + ip + "\"}");
                byte[] responseBytes = "{\"error\":\"Intrusion detected. Your IP has been logged and reported.\"}".getBytes("UTF-8");
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(403, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(responseBytes); }
            };
            server.createContext("/api/admin-database-backup", honeypotHandler);
            server.createContext("/api/honeypot", honeypotHandler);
            server.createContext("/api/admin", honeypotHandler);
            server.createContext("/api/wp-admin", honeypotHandler);

            server.createContext("/api/unblock-all", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    blockedIPs.clear();
                    broadcast("{\"type\":\"unblock_all\"}");
                    byte[] b = "{\"status\":\"UNBLOCKED_ALL\"}".getBytes("UTF-8");
                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                    exchange.sendResponseHeaders(200, b.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
                }
            });

            // 5. Dashboard (Allows authenticated operators to view SOC metrics)
            server.createContext("/api/dashboard", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Authorization");
                    
                    if ("OPTIONS".equals(exchange.getRequestMethod())) {
                        exchange.sendResponseHeaders(204, -1);
                        return;
                    }
                    
                    if (validateToken(exchange) == null) {
                        exchange.sendResponseHeaders(401, -1);
                        return;
                    }

                    int users = 0;
                    int events = 0;
                    int risk = calculateRiskScore();
                    String anomalies = checkAnomaly().isEmpty() ? "0" : "1";
                    
                    StringBuilder eventsList = new StringBuilder("[");
                    StringBuilder threatBreakdown = new StringBuilder("{");
                    
                    try (Connection con = getDbConnection()) {
                        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
                            if (rs.next()) users = rs.getInt(1);
                        }
                        
                        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM security_events")) {
                            if (rs.next()) events = rs.getInt(1);
                        }
                        
                        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM security_events ORDER BY event_time DESC LIMIT 10")) {
                            boolean first = true;
                            while (rs.next()) {
                                if (!first) eventsList.append(",");
                                eventsList.append("{\"type\":\"").append(rs.getString("event_type"))
                                          .append("\",\"details\":\"").append(rs.getString("details").replace("\"", "\\'"))
                                          .append("\",\"time\":\"").append(rs.getString("event_time")).append("\"}");
                                first = false;
                            }
                        }
                        
                        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT event_type, COUNT(*) as count FROM security_events GROUP BY event_type")) {
                            boolean first = true;
                            while (rs.next()) {
                                if (!first) threatBreakdown.append(",");
                                threatBreakdown.append("\"").append(rs.getString("event_type")).append("\":").append(rs.getInt("count"));
                                first = false;
                            }
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                    
                    eventsList.append("]");
                    threatBreakdown.append("}");
                    
                    StringBuilder blocked = new StringBuilder("[");
                    boolean fb = true;
                    for (String b : blockedIPs) {
                        if (!fb) blocked.append(",");
                        blocked.append("\"").append(b).append("\"");
                        fb = false;
                    }
                    blocked.append("]");

                    String response = String.format(
                        "{\"users\":%d, \"events\":%d, \"anomalies\":%s, \"risk\":%d, \"eventsList\":%s, \"threatBreakdown\":%s, \"blockedIPs\":%s}",
                        users, events, anomalies, risk, eventsList.toString(), threatBreakdown.toString(), blocked.toString()
                    );
                    
                    byte[] responseBytes = response.getBytes("UTF-8");
                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                    exchange.sendResponseHeaders(200, responseBytes.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(responseBytes);
                    os.close();
                }
            });

            // 6. Simulate
            server.createContext("/api/simulate", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "*");
                    
                    if ("OPTIONS".equals(exchange.getRequestMethod())) {
                        exchange.sendResponseHeaders(204, -1);
                        return;
                    }
                    
                    if (blockedIPs.contains(getClientIP(exchange))) {
                        exchange.sendResponseHeaders(403, -1);
                        return;
                    }

                    String query = exchange.getRequestURI().getQuery();
                    String msg = "ping";
                    if (query != null && query.contains("msg=")) {
                        msg = java.net.URLDecoder.decode(query.substring(query.indexOf("msg=") + 4), "UTF-8");
                    }

                    String aiJson = evaluateThreatWithAI(msg);
                    String reply = "Analysis completed.";
                    String riskLevel = "LOW";
                    String classification = "Normal Chat";
                    
                    try {
                        if (aiJson.contains("\"reply\"")) {
                            int start = aiJson.indexOf("\"", aiJson.indexOf(":", aiJson.indexOf("\"reply\"")) + 1);
                            int end = aiJson.indexOf("\"", start + 1);
                            if (start != -1 && end != -1) reply = aiJson.substring(start + 1, end);
                        }
                        if (aiJson.contains("\"risk_level\"")) {
                            int start = aiJson.indexOf("\"", aiJson.indexOf(":", aiJson.indexOf("\"risk_level\"")) + 1);
                            int end = aiJson.indexOf("\"", start + 1);
                            if (start != -1 && end != -1) riskLevel = aiJson.substring(start + 1, end);
                        }
                        if (aiJson.contains("\"classification\"")) {
                            int start = aiJson.indexOf("\"", aiJson.indexOf(":", aiJson.indexOf("\"classification\"")) + 1);
                            int end = aiJson.indexOf("\"", start + 1);
                            if (start != -1 && end != -1) classification = aiJson.substring(start + 1, end);
                        }
                    } catch (Exception e) {}

                    try (Connection con = getDbConnection(); PreparedStatement pst = con.prepareStatement("INSERT INTO chats (user_message, twin_reply) VALUES (?, ?)")) {
                        pst.setString(1, msg);
                        pst.setString(2, reply);
                        pst.executeUpdate();
                    } catch (Exception e) {}

                    if (!"LOW".equals(riskLevel)) {
                        logSecurityEvent("AI Threat: " + classification, "Source: Sandbox. Msg: " + msg);
                        broadcast("{\"type\":\"threat\", \"classification\":\"" + classification + "\", \"riskLevel\":\"" + riskLevel + "\"}");
                    }

                    String response = String.format("{\"reply\":\"%s\",\"riskLevel\":\"%s\",\"classification\":\"%s\"}", reply.replace("\"", "\\\""), riskLevel, classification);
                    byte[] responseBytes = response.getBytes("UTF-8");
                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                    exchange.sendResponseHeaders(200, responseBytes.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(responseBytes); }
                }
            });

            // 7. Isolate
            server.createContext("/api/isolate", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Authorization");
                    
                    if ("OPTIONS".equals(exchange.getRequestMethod())) {
                        exchange.sendResponseHeaders(204, -1);
                        return;
                    }
                    
                    if (blockedIPs.contains(getClientIP(exchange))) {
                        exchange.sendResponseHeaders(403, -1);
                        return;
                    }
                    
                    if (validateToken(exchange) == null) {
                        exchange.sendResponseHeaders(401, -1);
                        return;
                    }

                    String ip = "192.168.1." + (int)(Math.random()*255);
                    String mac = String.format("AA:BB:CC:%02X:%02X:%02X", (int)(Math.random()*255), (int)(Math.random()*255), (int)(Math.random()*255));
                    String details = "Kill switch activated. Blocked IP: " + ip + " | MAC: " + mac;
                    logSecurityEvent("SYSTEM ISOLATION", details);
                    
                    broadcast("{\"type\":\"isolation\", \"ip\":\"" + ip + "\", \"mac\":\"" + mac + "\"}");

                    String response = "{\"status\":\"ISOLATED\", \"ip\":\"" + ip + "\", \"mac\":\"" + mac + "\", \"timestamp\":\"" + java.time.LocalDateTime.now().toString() + "\", \"classification\":\"CRITICAL THREAT\"}";
                    byte[] responseBytes = response.getBytes();
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, responseBytes.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(responseBytes);
                    os.close();
                }
            });
            
            // 8. Static Files
            server.createContext("/", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {
                    String path = exchange.getRequestURI().getPath();
                    if (path.equals("/")) {
                        path = "/index.html";
                    }
                    File file = new File("public" + path);
                    if (!file.exists()) {
                        file = new File("D:/VSC Projects/CyberShadowTwinWeb" + path);
                    }
                    if (file.exists() && !file.isDirectory()) {
                        byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
                        if (path.endsWith(".html")) exchange.getResponseHeaders().set("Content-Type", "text/html");
                        else if (path.endsWith(".css")) exchange.getResponseHeaders().set("Content-Type", "text/css");
                        else if (path.endsWith(".js")) exchange.getResponseHeaders().set("Content-Type", "application/javascript");
                        exchange.sendResponseHeaders(200, bytes.length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(bytes);
                        os.close();
                    } else {
                        String res = "404 Not Found";
                        exchange.sendResponseHeaders(404, res.length());
                        OutputStream os = exchange.getResponseBody();
                        os.write(res.getBytes());
                        os.close();
                    }
                }
            });

            server.setExecutor(null);
            server.start();
            System.out.println("API Server started on port 8080");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        startApiServer();

        JFrame frame =
                new JFrame("Shadow Twin");
        frame.getContentPane().setBackground(
                new Color(5, 5, 5));

        JTextArea chatArea =
                new JTextArea();

        JScrollPane scroll =
                new JScrollPane(chatArea);

        JTextField inputField =
                new JTextField();

        JButton sendButton =
                new JButton("Send");
        JButton clearButton =
                new JButton("Clear");
        JButton exportButton =
                new JButton("Export");

        JLabel title =
                new JLabel("Cyber Shadow Twin");
        JLabel status =
                new JLabel("\uD83D\uDEE1 Monitoring");
        JLabel riskLabel =
                new JLabel("Risk Score: 0");

        JLabel eventLabel =
                new JLabel("Security Events: 0");

        JLabel anomalyLabel =
                new JLabel("Anomalies: 0");

        JLabel usersLabel=
                new JLabel("Users: 0");
        Font mainFont =
                new Font("Segoe UI", Font.PLAIN, 14);

        Font titleFont =
                new Font("Segoe UI", Font.BOLD, 28);

        title.setFont(titleFont);

        title.setForeground(
                Color.WHITE
        );
        riskLabel.setFont(mainFont);
        eventLabel.setFont(mainFont);
        anomalyLabel.setFont(mainFont);
        usersLabel.setFont(mainFont);
        status.setFont(mainFont);

        riskLabel.setForeground(Color.WHITE);
        eventLabel.setForeground(Color.WHITE);
        anomalyLabel.setForeground(Color.WHITE);
        usersLabel.setForeground(Color.WHITE);

        status.setForeground(
                new Color(154, 154, 154)
        );

        chatArea.setEditable(false);

        chatArea.setBackground(
                new Color(13, 13, 13)
        );

        chatArea.setForeground(
                new Color(220,220,220)

        );

        chatArea.setCaretColor(Color.WHITE);

        chatArea.setFont(
                new Font("Consolas", Font.PLAIN, 14)
        );

        frame.setLayout(null);

        title.setBounds(230, 10, 350, 35);
        status.setBounds(280, 45, 200, 20); // Centered under title
        
        scroll.setBounds(20,110,660,320);
        inputField.setBounds(20,455,430,35);

        sendButton.setBounds(470,455,90,35);
        clearButton.setBounds(570,455,90,35);
        exportButton.setBounds(570,520,90,35);

        // Perfectly aligned symmetric row for variables
        riskLabel.setBounds(20, 80, 150, 20);
        eventLabel.setBounds(180, 80, 160, 20);
        anomalyLabel.setBounds(350, 80, 150, 20);
        usersLabel.setBounds(510, 80, 150, 20);

        sendButton.setBorderPainted(false);
        clearButton.setBorderPainted(false);

        inputField.setBackground(
                new Color(5, 5, 5)
        );

        inputField.setForeground(
                Color.WHITE
        );

        inputField.setCaretColor(
                Color.WHITE
        );
        inputField.setBorder(
                BorderFactory.createLineBorder(
                        new Color(34, 34, 34),
                        1
                )
        );

        sendButton.setBackground(
                Color.WHITE
        );

        sendButton.setForeground(
                Color.BLACK
        );

        sendButton.setFocusPainted(false);

        clearButton.setBackground(
                new Color(255,70,70)
        );

        clearButton.setForeground(
                Color.WHITE
        );

        clearButton.setFocusPainted(false);

        exportButton.setBackground(
                new Color(0,180,120)
        );

        exportButton.setForeground(
                Color.WHITE
        );

        exportButton.setFocusPainted(false);

        exportButton.setBorderPainted(false);

        chatArea.append(
                "Shadow Twin: Hello 😄\n\n"
        );
        chatArea.setCaretPosition(
                chatArea.getDocument().getLength()
        );

        sendButton.addActionListener(e->{

            String message =
                    inputField.getText();

            String time =
                    LocalTime.now().format(
                            DateTimeFormatter.ofPattern(
                                    "hh:mm a"
                            )
                    );

            chatArea.append(
                    "["+time+"] You: "
                            +message
                            +"\n"
            );
            chatArea.setCaretPosition(
                    chatArea.getDocument().getLength()
            );

            status.setText("🤖 Thinking...");
            chatArea.append("Shadow Twin is typing...\n");

            chatArea.setCaretPosition(
                    chatArea.getDocument().getLength()
            );


            inputField.setText("");

            Timer timer =
                    new Timer(
                            1000,
                            ev -> {

                                String reply =
                                        getReply(message);

                                try{

                                    Connection con=
                                            getDbConnection();

                                    PreparedStatement pst=
                                            con.prepareStatement(
                                                    "INSERT INTO chats(user_message,twin_reply) VALUES(?,?)"
                                            );

                                    pst.setString(
                                            1,
                                            message
                                    );

                                    pst.setString(
                                            2,
                                            reply
                                    );

                                    pst.executeUpdate();

                                    con.close();

                                }

                                catch(Exception ex){

                                    System.out.println(ex);
                                }

                                chatArea.append(
                                        "Shadow Twin: "
                                                +reply
                                                +"\n"
                                );
                                chatArea.setCaretPosition(
                                        chatArea.getDocument().getLength()
                                );

                                String anomaly =
                                        checkAnomaly();
                                int risk=
                                        calculateRiskScore();
                                riskLabel.setText(
                                        "Risk Score: "+risk
                                );

                                eventLabel.setText(
                                        "Security Events: "
                                                +getEventCount()
                                );

                                if(!anomaly.equals("")){

                                    anomalyLabel.setText(
                                            "Anomalies: 1"
                                    );
                                }
                                if(risk>=10){

                                    status.setText(
                                            "🚨 Critical Threat"
                                    );

                                    chatArea.append(
                                            "Cyber Shadow Twin: 🚨 CRITICAL SECURITY ALERT\n"
                                                    +"Multiple risky patterns detected\n\n"
                                    );
                                    chatArea.setCaretPosition(
                                            chatArea.getDocument().getLength()
                                    );
                                }

                                else if(risk>=8){

                                    status.setText("🔴 High Risk Detected");
                                }

                                else if(risk>=4){

                                    status.setText("🟡 Medium Risk Activity");
                                }

                                else{

                                    status.setText("🟢 Monitoring Safe");
                                }


                                String riskLevel="🟢 Low Risk";

                                if(risk>=4 && risk<=7){

                                    riskLevel="🟡 Medium Risk";
                                }

                                else if(risk>=8){

                                    riskLevel="🔴 High Risk";
                                }

                                if(!anomaly.equals("")){

                                    chatArea.append(
                                            "Shadow Twin: "
                                                    +anomaly
                                                    +"\n"
                                    );
                                    chatArea.setCaretPosition(
                                            chatArea.getDocument().getLength()
                                    );
                                }

                                chatArea.append(
                                        "Risk Score: "
                                                +risk
                                                +" | "
                                                +riskLevel
                                                +"\n\n"
                                );
                                chatArea.setCaretPosition(
                                        chatArea.getDocument().getLength()
                                );



                                chatArea.append("\n");

                                chatArea.setCaretPosition(
                                        chatArea.getDocument().getLength()
                                );

                                ((Timer)ev.getSource()).stop();

                            }
                    );

            timer.start();

        });

        exportButton.addActionListener(e->{

            try{

                PrintWriter writer =
                        new PrintWriter(
                                new FileWriter(
                                        System.getProperty("user.home")
                                                + "\\Desktop\\ShadowTwin_Report_"
                                                +
                                                java.time.LocalDateTime.now()
                                                        .toString()
                                                        .replace(":","-")
                                                +
                                                ".txt"
                                )
                                
                        );

                writer.println(
                        "===== CYBER SHADOW TWIN REPORT ====="
                );

                writer.println();

                writer.println(
                        "Generated: "
                                + java.time.LocalDateTime.now()
                );

                writer.println();

                writer.println(
                        "Total Security Events : "
                                + eventLabel.getText()
                );

                writer.println(
                        "Risk Score            : "
                                + riskLabel.getText()
                );

                writer.println(
                        "Anomalies             : "
                                + anomalyLabel.getText()
                );

                writer.println(
                        "Users                 : "
                                + usersLabel.getText()
                );

                writer.println();

                writer.println(
                        "===================================="
                );

                writer.println(
                        "RECENT SECURITY EVENTS"
                );

                writer.println(
                        "===================================="
                );

                writer.println();

                writer.println(
                        chatArea.getText()
                );

                writer.close();

                File file =
                        new File(
                                "ShadowTwin_Report.txt"
                        );

                chatArea.append(
                        "\n📄 Report exported:\n"
                                + file.getAbsolutePath()
                                + "\n"
                );

                chatArea.setCaretPosition(
                        chatArea.getDocument().getLength()
                );

            }

            catch(Exception ex){

                chatArea.append(
                        "\n❌ Export failed.\n"
                );
            }

        });

        clearButton.addActionListener(e->{

            chatArea.setText(
                    "Shadow Twin: Chat cleared 😄\n\n" +"Shadow Twin: Hello devesh 👋\n\n"
            );

        });
        frame.add(title);
        frame.add(status);

        frame.add(riskLabel);
        frame.add(eventLabel);
        frame.add(anomalyLabel);
        frame.add(usersLabel);

        frame.add(scroll);

        frame.add(inputField);
        frame.add(sendButton);
        frame.add(clearButton);
        frame.add(exportButton);



        try{

            Connection con=
                    getDbConnection();

            Statement st=
                    con.createStatement();

            ResultSet rs=
                    st.executeQuery(
                            "SELECT COUNT(*) AS totalUsers FROM users"
                    );

            if(rs.next()){

                usersLabel.setText(
                        "Users: "
                                +
                                rs.getInt(
                                        "totalUsers"
                                )
                );
            }

        }
        catch(Exception e){

            System.out.println(e);

        }

        refreshDashboard(
                usersLabel,
                eventLabel,
                anomalyLabel,
                chatArea
        );

        frame.setSize(720, 650);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);


        frame.setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
        );

    }

    public static String checkAnomaly(){

        try{

            Connection con=
                    getDbConnection();

            Statement st=
                    con.createStatement();

            ResultSet rs=
                    st.executeQuery(

                            "SELECT COUNT(*) AS total FROM security_events"

                    );

            if(rs.next()){

                int count=
                        rs.getInt("total");

                con.close();

                if(count>=3){

                    return "🚨 Anomaly detected: Repeated risky activity found.";

                }

            }

            con.close();

        }

        catch(Exception e){

            System.out.println(e);

        }

        return "";
    }


    public static int calculateRiskScore(){

        int score=0;

        try{

            Connection con=
                    getDbConnection();

            Statement st=
                    con.createStatement();

            ResultSet rs=
                    st.executeQuery(
                            "SELECT event_type, details FROM security_events"
                    );

            while(rs.next()){

                String event=
                        rs.getString("details");
                String type=
                        rs.getString("event_type");
                
                if (type == null) type = "";
                if (event == null) event = "";

                if(event.equals("Password too short"))
                    score+=2;
                else if(event.equals("Numeric-only password"))
                    score+=2;
                else if(event.equals("Common password"))
                    score+=3;
                else if(event.equals("Strong password"))
                    score-=1;
                    
                if(type.startsWith("AI Threat:"))
                    score+=15;
                if(type.equals("SYSTEM ISOLATION"))
                    score+=50;
            }

            con.close();

        }

        catch(Exception e){

            System.out.println(e);

        }

        return score;
    }

    public static int getEventCount(){

        int count=0;

        try{

            Connection con=
                    getDbConnection();

            Statement st=
                    con.createStatement();

            ResultSet rs=
                    st.executeQuery(
                            "SELECT COUNT(*) AS total FROM security_events"
                    );

            if(rs.next()){

                count=
                        rs.getInt("total");
            }

            con.close();

        }

        catch(Exception e){

            System.out.println(e);
        }

        return count;
    }

    public static void logSecurityEvent(
            String type,
            String details
    ){

        try{

            Connection con=
                    getDbConnection();

            PreparedStatement pst=
                    con.prepareStatement(

                            "INSERT INTO security_events(event_type,details) VALUES(?,?)"

                    );

            pst.setString(1,type);
            pst.setString(2,details);

            pst.executeUpdate();

            con.close();

        }

        catch(Exception e){

            System.out.println(e);

        }

    }
    public static void refreshDashboard(
            JLabel usersLabel,
            JLabel eventLabel,
            JLabel anomalyLabel,
            JTextArea chatArea
    ){

        try{

            Connection con=
                    getDbConnection();

            Statement st=
                    con.createStatement();

            // USERS
            ResultSet users=
                    st.executeQuery(
                            "SELECT COUNT(*) AS total FROM users"
                    );

            if(users.next()){

                usersLabel.setText(
                        "Users: "
                                +
                                users.getInt("total")
                );
            }

            // EVENTS
            ResultSet events=
                    st.executeQuery(
                            "SELECT COUNT(*) AS total FROM security_events"
                    );

            if(events.next()){

                eventLabel.setText(
                        "Security Events: "
                                +
                                events.getInt("total")
                );
            }

            // ANOMALY
            ResultSet anomaly=
                    st.executeQuery(
                            "SELECT COUNT(*) AS total FROM security_events"
                    );

            if(anomaly.next()){

                int total=
                        anomaly.getInt("total");

                if(total>=3){

                    anomalyLabel.setText(
                            "Anomalies: 1"
                    );
                }

                else{

                    anomalyLabel.setText(
                            "Anomalies: 0"
                    );
                }
            }

            // LIVE LOGS
            ResultSet logs=
                    st.executeQuery(
                            "SELECT * FROM security_events ORDER BY id DESC LIMIT 10"
                    );

            chatArea.setText("");

            while(logs.next()){

                String type=
                        logs.getString("event_type");

                String details=
                        logs.getString("details");

                String time=
                        logs.getString("event_time");

                chatArea.append(
                        "["+time+"] "
                                +type+
                                " : "
                                +details+
                                "\n\n"
                );
                chatArea.setCaretPosition(
                        chatArea.getDocument().getLength()
                );
            }

            con.close();

        }

        catch(Exception e){

            System.out.println(e);

        }
    }
}