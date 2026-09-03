import re

with open(r"D:\intelli J Project\untitled\src\ShadowTwinUI.java", "r", encoding="utf-8") as f:
    content = f.read()

config_block = """
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
"""

content = content.replace("public class ShadowTwinUI {", "public class ShadowTwinUI {\n" + config_block, 1)

pattern = r'DriverManager\.getConnection\(\s*"jdbc:mysql://localhost:3306/shadow_twin_db"\s*,\s*"root"\s*,\s*"Muggi@123"\s*\)'
content = re.sub(pattern, 'getDbConnection()', content)

with open(r"D:\intelli J Project\untitled\src\ShadowTwinUI.java", "w", encoding="utf-8") as f:
    f.write(content)

print("Refactoring complete.")
