/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ServerSide;

import java.io.IOException;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.net.Socket;

public class Server {

    private static HikariDataSource dataSource;

    // Initialize the connection pool
    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://127.0.0.1:5432/Library");
        config.setUsername("postgres");
        config.setPassword("hilaData123");
        config.setMaximumPoolSize(10);  // Set maximum pool size based on your requirements
        dataSource = new HikariDataSource(config);
    }

    public static void main(String[] args) {
        try (Connection con = dataSource.getConnection()) {
            // Table setup
            setupDatabase(con);

            // Start the server
            try (ServerSocket serverSocket = new ServerSocket(1234)) {
                System.out.println("Server running");

                while (true) {
                    System.out.println("Waiting for client...");
                    Socket socket = serverSocket.accept();
                    System.out.println("Client connected.");

                    // Pass the socket and the dataSource to a new ClientHandler thread
                    new Thread(new ClientHandler(socket, dataSource)).start();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } catch (SQLException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (dataSource != null) {
                dataSource.close(); // Close the pool on shutdown
            }
        }
    }

    private static void setupDatabase(Connection con) throws SQLException {
        boolean isNewDatabase = false;

        // Create "Users" table if it doesn't exist
        try (PreparedStatement ps1 = con.prepareStatement(
                "CREATE TABLE IF NOT EXISTS public.\"Users\" ("
                + "\"userId\" serial NOT NULL, "
                + "username text NOT NULL, "
                + "password text NOT NULL, "
                + "PRIMARY KEY (\"userId\"))")) {

            // Execute the statement to create the table if it doesn't exist
            ps1.executeUpdate();

            // Check if the table is empty, indicating a new database setup
            try (PreparedStatement checkEmpty = con.prepareStatement(
                    "SELECT COUNT(*) FROM public.\"Users\""); ResultSet rs = checkEmpty.executeQuery()) {

                if (rs.next() && rs.getInt(1) == 0) {
                    isNewDatabase = true;
                }
            }
        }

        // Insert the admin user only if this is a new database
        if (isNewDatabase) {
            try (PreparedStatement ps2 = con.prepareStatement(
                    "INSERT INTO public.\"Users\" (username, password) VALUES (?, ?)")) {

                ps2.setString(1, "ADMIN");
                ps2.setString(2, "Admin");
                ps2.executeUpdate();
                System.out.println("Admin initialization successful.\nPassword is: Admin");
            }
        } else {
            System.out.println("Users table already exists. Skipping admin initialization.");
        }

        // Create "Books" table if it doesn't exist
        try (PreparedStatement ps3 = con.prepareStatement(
                "CREATE TABLE IF NOT EXISTS public.\"Books\" ("
                + "\"bookId\" serial NOT NULL, "
                + "title text NOT NULL, "
                + "author text NOT NULL, "
                + "genres text[] NOT NULL, "
                + "book_address text NOT NULL, "
                + "cover bytea NOT NULL, "
                + "PRIMARY KEY (\"bookId\"))")) {

            ps3.executeUpdate();
            System.out.println("Books table setup completed.");
        }

        // Create "DownloadedBooks" table if it doesn't exist
        try (PreparedStatement ps4 = con.prepareStatement(
                "CREATE TABLE IF NOT EXISTS public.\"DownloadedBooks\" ("
                + "\"transactionId\" serial NOT NULL, "
                + "\"userId\" integer NOT NULL, "
                + "\"bookId\" integer NOT NULL, "
                + "\"dateDownloaded\" date NOT NULL, "
                + "PRIMARY KEY (\"transactionId\"))")) {

            ps4.executeUpdate();
            System.out.println("DownloadedBooks table setup completed.");
        }
    }
}
