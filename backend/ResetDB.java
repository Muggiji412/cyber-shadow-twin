import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;
import java.io.FileInputStream;

public class ResetDB {
    public static void main(String[] args) {
        try {
            Properties config = new Properties();
            FileInputStream fis = new FileInputStream("config.properties");
            config.load(fis);
            fis.close();
            
            Connection con = DriverManager.getConnection(
                config.getProperty("db.url", "jdbc:mysql://localhost:3306/shadow_twin_db"),
                config.getProperty("db.user", "root"),
                config.getProperty("db.password", "Muggi@123")
            );
            Statement st = con.createStatement();
            st.executeUpdate("TRUNCATE TABLE security_events");
            st.executeUpdate("TRUNCATE TABLE chats");
            con.close();
            System.out.println("Reset successful.");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
