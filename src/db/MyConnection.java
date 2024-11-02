/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package db;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 *
 * @author Hila
 */
public class MyConnection {
   private static final String username = "your database's username";
    private static final String password = "your database's password";
    private static final String dataConn = "your database's path";
    private static Connection con = null;
    

    public static Connection getConnection() {
        try {
            con = DriverManager.getConnection(dataConn, username, password);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return con;
    }

}
