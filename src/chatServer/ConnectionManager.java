package chatServer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionManager {

    public static Connection getConnection(String dbUsername, String dbPassword) throws SQLException, ClassNotFoundException {
        String dbDriver = "com.mysql.jdbc.Driver";
        String dbURL = ServerConfig.DB_URL;
        String dbName = ServerConfig.DB_NAME;
        Class.forName(dbDriver);
        Connection con = DriverManager.getConnection(dbURL + dbName+"?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC", dbUsername, dbPassword);
        return con;
    }
}
