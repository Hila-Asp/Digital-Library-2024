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
import java.io.File;
import java.io.FileWriter;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;
import org.json.JSONObject;

public class Server {

    private static final ReentrantLock cacheLock = new ReentrantLock(); // Lock to ensure only one thread updates the cache at a time
    
    private static final HikariDataSource dataSource;
    private static final String CACHE_FILE_PATH = "book_cache.json"; //change to where you want it ti be saved. currently it is  saved in the project folder.
    // Initialize the connection pool
    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://127.0.0.1:5432/Library");
        config.setUsername("postgres");
        config.setPassword("hilaData123");
        config.setMaximumPoolSize(10);  // maximum pool size
        dataSource = new HikariDataSource(config);
    }

    public static void main(String[] args) {
        try (Connection con = dataSource.getConnection()) {

            deleteJsonFileIfExists();
            createNewJsonFile();

            setupDatabase(con);

            // Start the server
            try (ServerSocket serverSocket = new ServerSocket(1234)) {
                System.out.println("Server running");

                while (true) {
                    System.out.println("Waiting for client...");
                    Socket socket = serverSocket.accept();
                    System.out.println("Client connected.");

                    // Pass the socket and the dataSource to a new ClientHandler thread
                    new Thread(new ClientHandler(socket, dataSource, CACHE_FILE_PATH, cacheLock)).start();
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

    private static void setupDatabase(Connection con) throws SQLException { // creates the tables and insert the admin if the users table doesnt exist.
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
            try (PreparedStatement checkEmpty = con.prepareStatement("SELECT COUNT(*) FROM public.\"Users\""); ResultSet rs = checkEmpty.executeQuery()) {

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

    // Method to create a new JSON file
    public static void createNewJsonFile() {
        try {
            // Create an empty JSON object
            JSONObject json = new JSONObject();

            // Write the empty JSON object to the file
            try (FileWriter file = new FileWriter(CACHE_FILE_PATH)) {
                file.write(json.toString(4)); // Indent for readability
                System.out.println("Created new JSON file: " + CACHE_FILE_PATH);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteJsonFileIfExists() {
        File file = new File(CACHE_FILE_PATH);
        if (file.exists() && file.delete()) {
            System.out.println("JSON file deleted on startup.");
        }
    }
}
