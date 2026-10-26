
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Manages JDBC connections for the academic evaluation database. */
public class DBConnection {

    // Read configuration from environment variables.
    private static String getEnvValue(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value == null || value.isEmpty()) ? defaultValue : value;
    }

    // Load the MySQL JDBC driver when the class is initialized.
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            System.err.println(
                "[ERROR] Unable to load MySQL JDBC driver: " + ex.getMessage()
            );
        }
    }

    // Establish and return a database connection.
    public static Connection getConnection() throws SQLException {

        String host = getEnvValue("DB_HOST", "localhost");
        String port = getEnvValue("DB_PORT", "3306");
        String database = getEnvValue("DB_NAME", "kec_eval");

        String username = getEnvValue("DB_USER", "root");
        String password = getEnvValue("DB_PASSWORD", "root");

        String connectionUrl = "jdbc:mysql://" + host + ":" + port + "/"
                + database
                + "?useSSL=false"
                + "&allowPublicKeyRetrieval=true"
                + "&serverTimezone=UTC";

        return DriverManager.getConnection(
                connectionUrl, username, password
        );
    }

    // Verify whether the database connection is available.
    public static boolean testConnection() {
        try (Connection connection = getConnection()) {
            return connection != null && !connection.isClosed();

        } catch (SQLException ex) {
            System.err.println(
                "[ERROR] Database connection failed: " + ex.getMessage()
            );
            return false;
        }
    }
}

