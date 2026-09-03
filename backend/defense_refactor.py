import re

with open(r"D:\intelli J Project\untitled\src\ShadowTwinUI.java", "r", encoding="utf-8") as f:
    content = f.read()

isolate_endpoint = """
            server.createContext("/api/isolate", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                    
                    if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                        exchange.sendResponseHeaders(204, -1);
                        return;
                    }
                    
                    try {
                        Connection con = getDbConnection();
                        PreparedStatement logPst = con.prepareStatement("INSERT INTO security_events(event_type, details) VALUES(?,?)");
                        logPst.setString(1, "SYSTEM ISOLATION");
                        logPst.setString(2, "Kill switch activated. Target blocked.");
                        logPst.executeUpdate();
                        con.close();
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                    
                    String response = "{\\"status\\": \\"ISOLATED\\"}";
                    byte[] responseBytes = response.getBytes();
                    exchange.sendResponseHeaders(200, responseBytes.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(responseBytes);
                    os.close();
                }
            });

            server.setExecutor(null);"""

content = content.replace("            server.setExecutor(null);", isolate_endpoint)

with open(r"D:\intelli J Project\untitled\src\ShadowTwinUI.java", "w", encoding="utf-8") as f:
    f.write(content)
