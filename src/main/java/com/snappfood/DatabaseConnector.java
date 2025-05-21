package com.snappfood;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnector {
    private static final String URL = "jdbc:mysql://localhost:3306/snappfood";
    private static final String USER = "root";
    private static final String PASSWORD = "Radman1385";

    public static void main(String[] args) {
        try {
            Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Connected to MySQL successfully!");
            connection.close();
        } catch (SQLException e) {
            System.out.println("❌ Failed to connect to MySQL.");
            e.printStackTrace();
        }
    }
}