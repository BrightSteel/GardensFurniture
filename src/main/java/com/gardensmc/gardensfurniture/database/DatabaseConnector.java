package com.gardensmc.gardensfurniture.database;

import com.gardensmc.gardensplayers.GardensPlayers;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.*;
import java.util.Objects;

public class DatabaseConnector {

    public static final DatabaseConnector INSTANCE = new DatabaseConnector();
//    private final HikariDataSource dataSource;
//    public HikariConfig hikariConfig = new HikariConfig();

    public static Connection connection;

    public DatabaseConnector() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
//        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
//        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
//        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
//        hikariConfig.setMaximumPoolSize(1);
//        hikariConfig.setMinimumIdle(1);
        String url = "jdbc:sqlite:" + Bukkit.getServer().getWorldContainer() + "/furniture.db";
        try {
            connection = DriverManager.getConnection(url);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
//        hikariConfig.setJdbcUrl(url);
//        dataSource = new HikariDataSource(hikariConfig);
    }

    public Connection getConnection() throws SQLException {
        return connection;
    }

    public void executeUpdate(String string, Object... obj) throws SQLException {



        PreparedStatement ps = connection.prepareStatement(string);
        for(int i = 0; i < obj.length; i++) {
            ps.setObject(i + 1, obj[i]);
        }

        ps.executeUpdate();
        ps.close();
//        connection.close();
    }

    public boolean queryPresence(String string, Object... obj) throws SQLException {
        Connection con = connection;
        PreparedStatement ps = con.prepareStatement(string);
        for (int i = 0; i < obj.length; i++) {
            ps.setObject(i + 1, obj[i]);
        }
        ResultSet rs = ps.executeQuery();
        boolean present = rs.next();
        rs.close();
        ps.close();
//        con.close();
        return present;
    }

    public boolean connectTest() {
//        try {
//        } catch (SQLException throwables) {
//            return false;
//        }
        return true;
    }
}
