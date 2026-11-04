package mx.proyecto.pos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {
    private static final String URL = setting("DB_URL", "jdbc:mysql://localhost:3306/pos_transacciones?serverTimezone=America/Mexico_City");
    private static final String USER = setting("DB_USER", "root");
    private static final String PASSWORD = setting("DB_PASSWORD", "WantedU1:08");

    private Database() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    private static String setting(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null ? defaultValue : value;
    }
}
