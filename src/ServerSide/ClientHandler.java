/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ServerSide;

import java.awt.Image;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Array;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubReader;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.mindrot.jbcrypt.BCrypt;

/**
 *
 * @author Hila
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final DataSource dataSource;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private ObjectOutputStream objectOutputStream;
    private String username;
    private final String cache_file_path;
    private final ReentrantLock cacheLock;
    

    public ClientHandler(Socket socket, DataSource dataSource, String cache_file_path, ReentrantLock cacheLock) {
        this.socket = socket;
        this.dataSource = dataSource;
        this.cache_file_path = cache_file_path;
        this.cacheLock = cacheLock;

    }

    @Override
    public void run() {
        try {
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());

            // Get the username from the client
            int usernameLength = dataInputStream.readInt();
            byte[] usernameBytes = new byte[usernameLength];
            dataInputStream.readFully(usernameBytes);
            username = new String(usernameBytes);
            System.out.println("Username: " + username);

            String action;
            while (true) {
                int actionLength = dataInputStream.readInt();
                if (actionLength > 0) {
                    byte[] actionBytes = new byte[actionLength];
                    dataInputStream.readFully(actionBytes);
                    action = new String(actionBytes);

                    // Handle actions based on the string received
                    switch (action) {
                        case "getUserBooks" ->
                            getUserBooks();
                        case "getUserDownloads" ->
                            getDownloads();
                        case "username" ->
                            updateUsername();
                        case "password" ->
                            updatePassword();
                        case "download" ->
                            download();
                        case "preview" ->
                            preview();
                        case "getUsers" ->
                            getUsers();
                        case "addUser" ->
                            addUser();
                        case "updateUser" ->
                            updateUser();
                        case "deleteUser" ->
                            deleteUser();
                        case "userExists" ->
                            userExists();
                        case "getPassword" ->
                            getPassword();
                        case "canUpdateUser" ->
                            canUpdateUser();
                        case "getNextId" ->
                            getNextUserId();
                        case "addBook" ->
                            addBook();
                        case "updateBook" ->
                            updateBook();
                        case "deleteBook" ->
                            deleteBook();
                        case "getBooks" ->
                            getBooks();
                        case "getNextBookId" ->
                            getNextBookId();
                        case "bookExists" ->
                            bookExists();
                        case "getDownloadedBooks" ->
                            getDownloadedBooks();
                        default ->
                            System.out.println("Unknown action: " + action);
                    }
                }
            }
        } catch (IOException ex) {
            System.out.println("Client disconnected: " + username);
        } finally {
            // Cleanup resources
            try {
                if (dataInputStream != null) {
                    dataInputStream.close();
                }
                if (dataOutputStream != null) {
                    dataOutputStream.close();
                }
                if (objectOutputStream != null) {
                    objectOutputStream.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // <editor-fold defaultstate="collapsed" desc="Mange User Form Code"> 
    private void getUserBooks() {
        String sql = "select title, author, genres, cover from public.\"Books\" where concat(title, author) ilike ? order by title";
        try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            int searchValueLength = dataInputStream.readInt();
            byte[] searchValueBytes = new byte[searchValueLength];
            dataInputStream.readFully(searchValueBytes);
            String searchValue = new String(searchValueBytes);

            ArrayList<Object[]> table = new ArrayList<>();

            ps.setString(1, "%" + searchValue + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                byte[] imageData = rs.getBytes(4);
                ImageIcon imageIcon = new ImageIcon(imageData);
                Image image = imageIcon.getImage().getScaledInstance(133, 200, Image.SCALE_SMOOTH);
                imageIcon = new ImageIcon(image);
                JLabel lebl = new JLabel("");
                lebl.setIcon(imageIcon);
                lebl.setHorizontalAlignment(JLabel.CENTER);
                table.add(new Object[]{rs.getString(1), rs.getString(2), (String[]) rs.getArray(3).getArray(), lebl, "Preview First Chapter", "Download"});
            }
            objectOutputStream.writeObject(table);
            objectOutputStream.flush();
        } catch (SQLException | IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void getDownloads() {
        String sql = "SELECT public.\"Books\".title, public.\"Books\".author, public.\"Books\".genres, public.\"Books\".cover "
                + "FROM Public.\"DownloadedBooks\" "
                + "JOIN public.\"Books\" ON public.\"Books\".\"bookId\" = public.\"DownloadedBooks\".\"bookId\" "
                + "WHERE public.\"DownloadedBooks\".\"userId\"=?"
                //+ " AND concat (title, author) LIKE ?"
                + " ORDER BY Public.\"Books\".title";
        try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            int id = getUserId();
            if (id == -1) {
                return;
            }
            ArrayList<Object[]> table = new ArrayList<>();

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                byte[] imageData = rs.getBytes(4);
                ImageIcon imageIcon = new ImageIcon(imageData);
                Image image = imageIcon.getImage().getScaledInstance(133, 200, Image.SCALE_SMOOTH);
                imageIcon = new ImageIcon(image);
                JLabel lebl = new JLabel("");
                lebl.setIcon(imageIcon);
                lebl.setHorizontalAlignment(JLabel.CENTER);
                table.add(new Object[]{rs.getString(1), rs.getString(2), (String[]) rs.getArray(3).getArray(), lebl, "Download Again"});
            }
            objectOutputStream.writeObject(table);
            objectOutputStream.flush();
        } catch (SQLException | IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void download() {
        String sql = "Select book_address from public.\"Books\" where title=?";
        try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            int bookTitleLength = dataInputStream.readInt();
            if (bookTitleLength > 0) {
                byte[] bookTitleBytes = new byte[bookTitleLength];
                dataInputStream.readFully(bookTitleBytes);
                String bookTitle = new String(bookTitleBytes);

                ps.setString(1, bookTitle);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String bookPath = rs.getString(1);
                    File file = new File(bookPath);
                    if (file.exists()) {
                        String fileName = file.getName();
                        byte[] fileNameBytes = fileName.getBytes();
                        byte[] fileBytes = java.nio.file.Files.readAllBytes(file.toPath());

                        dataOutputStream.writeInt(fileNameBytes.length);
                        dataOutputStream.write(fileNameBytes);
                        dataOutputStream.writeInt(fileBytes.length);
                        dataOutputStream.write(fileBytes);

                        addDownloadedBook(bookTitle);
                    } else {
                        System.out.println("File not found: " + bookTitle);
                    }
                }
            }
        } catch (SQLException | IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void preview() {
        String sql = "Select book_address from public.\"Books\" where title=?";
        try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            int bookTitleLength = dataInputStream.readInt();
            if (bookTitleLength > 0) {
                byte[] bookTitleBytes = new byte[bookTitleLength];
                dataInputStream.readFully(bookTitleBytes);
                String bookTitle = new String(bookTitleBytes);

                byte[] bookCache = getPreviewFromCache(bookTitle);
                if (bookCache != null) {
                    System.out.println("Found book in cache");

                    dataOutputStream.writeInt(bookCache.length);
                    dataOutputStream.write(bookCache);
                    return;
                }

                ps.setString(1, bookTitle);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String bookPath = rs.getString(1);

                    File file = new File(bookPath);
                    if (file.exists()) {
                        InputStream epubInputStream = Files.newInputStream(file.toPath());
                        Book book = (new EpubReader()).readEpub(epubInputStream);

                        int firstChapterIndex = findFirstChapterIndex(book);
                        if (firstChapterIndex == -1) {
                            System.err.println("No valid chapter found in the book.");
                            return;
                        }
                        // Get the first chapter resource
                        Resource firstChapter = book.getSpine().getResource(firstChapterIndex);
                        byte[] firstChapterBytes = firstChapter.getData();

                        boolean added = addPreviewToCache(bookTitle, firstChapterBytes);
                        if (added) {
                            System.out.println(bookTitle + " added to the cache");
                        }

                        dataOutputStream.writeInt(firstChapterBytes.length);
                        dataOutputStream.write(firstChapterBytes);

                    } else {
                        System.out.println("File not found: " + bookTitle);
                    }

                }
            }
        } catch (SQLException | IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // <editor-fold defaultstate="collapsed" desc="Cache Code">
    private byte[] getPreviewFromCache(String title) {
        try {
            String jsonFileContent = Files.readString(Paths.get(cache_file_path));
            JSONObject jsonObject = new JSONObject(jsonFileContent);

            // Retrieve and decode the value of "Cpnder"
            if (jsonObject.has(title)) {
                String encodedBytes = jsonObject.getString(title);
                byte[] decodedBytes = Base64.getDecoder().decode(encodedBytes);
                return decodedBytes;
            } else {
                System.out.println(title + " key not found in JSON.");
                return null;
            }
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private boolean addPreviewToCache(String title, byte[] content) {
        // Acquire lock before modifying the cache
        cacheLock.lock();
        try {
            // Read the existing cache file
            String jsonFileContent = Files.readString(Paths.get(cache_file_path));
            JSONObject jsonObject = new JSONObject(jsonFileContent);

            // Add the book preview to the JSON object
            jsonObject.put(title, Base64.getEncoder().encodeToString(content));

            // Write back to the file with indentation for readability
            try (FileWriter file = new FileWriter(cache_file_path)) {
                file.write(jsonObject.toString(4)); // Indent for readability
            }

            // Create a new thread to delete the entry after 5 minutes
            new Thread(() -> {
                try {
                    // Sleep for 2 hours
                    Thread.sleep(120 * 60 * 1000);
                } catch (InterruptedException e) {
                    // Handle thread interruption gracefully
                    System.out.println("Thread interrupted while sleeping for cache deletion.");
                    return; // Exit the thread if interrupted
                }

                // Delete the entry from the cache
                deletePreviewFromCache(title);
            }).start();

            return true;
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            // Release the lock after modifying the cache
            cacheLock.unlock();
        }
        return false;
    }

    private void deletePreviewFromCache(String title) {
        // Acquire lock before modifying the cache
        cacheLock.lock();
        try {
            // Read the existing cache file
            String jsonFileContent = Files.readString(Paths.get(cache_file_path));
            JSONObject jsonObject = new JSONObject(jsonFileContent);

            // Check if the title exists in the cache and remove it
            if (jsonObject.has(title)) {
                jsonObject.remove(title);

                // Write the updated content back to the file
                try (FileWriter file = new FileWriter(cache_file_path)) {
                    file.write(jsonObject.toString(4)); // Indent for readability
                }
                System.out.println("Preview for " + title + " deleted from cache after 5 minutes.");
            } else {
                System.out.println("No cache entry found for " + title + " to delete.");
            }
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            // Release the lock after modifying the cache
            cacheLock.unlock();
        }
    }

// </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="First Chapter Code">
    private static int findFirstChapterIndex(Book book) throws IOException {
        // Iterate through all resources in the spine of the EPUB
        for (int i = 0; i < book.getSpine().size(); i++) {
            Resource resource = book.getSpine().getResource(i);

            // Only process HTML/XHTML resources (likely to contain chapter content)
            if (resource.getHref().toLowerCase().endsWith(".html") || resource.getHref().toLowerCase().endsWith(".xhtml")) {
                String content = new String(resource.getData());
                Document doc = Jsoup.parse(content);

                // Debug: Print the first 500 characters of each resource to inspect
                //System.out.println("Resource " + i + "\nSize: " + resource.getSize() + "\ncontent:\n" + doc.text().substring(0, Math.min(500, doc.text().length())));
                //System.out.println("");
                // because the format of each book is diffrent, it is almost impossible to fine for each book its first chapter
                // the only thing that is the same for all is that the size of the resource is bigger than the resources that were before
                // except for the table of contents whos size is sometimes even larger than some chapters.
                // therefore i check if the resource is not a table of content and its size is bigger than 5000.
                // (i checked 12 books and in all 12 the first chapter was larger than 5000 and the resources before were smaller than 5000)
                if (!isTableOfContents(doc) && resource.getSize() > 5000) {
                    return i;  // Found the first valid chapter
                }
            }
        }
        return -1;  // No valid chapter found
    }

// Function to detect if the content is part of the table of contents
    private static boolean isTableOfContents(Document doc) {
        String contentText = doc.text();

        // Check if it contains the word 'Contents' or if there are a large number of links (typically in TOC)
        if (contentText.contains("Contents") || doc.select("a[href]").size() > 5) {
            return true;
        }
        return false;
    }// </editor-fold>

    private void updateUsername() {
        String sql = "update public.\"Users\" set username =? where \"userId\"=?";
        try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            String newUsername = dataInputStream.readUTF();
            System.out.println(newUsername);
            int id = getUserId();
            if (userExists(newUsername)) {
                byte[] msgBytes = "This username is already used".getBytes();
                dataOutputStream.writeInt(msgBytes.length);
                dataOutputStream.write(msgBytes);
                dataOutputStream.flush();
                return;
            } else if (id == -1) {
                byte[] msgBytes = "can't find the user to change it's username".getBytes();
                dataOutputStream.writeInt(msgBytes.length);
                dataOutputStream.write(msgBytes);
                dataOutputStream.flush();
                return;
            }
            ps.setString(1, newUsername);
            ps.setInt(2, id);
            System.out.println(id);
            if (ps.executeUpdate() > 0) {
                byte[] msgBytes = "Username changed successfully".getBytes();
                username = newUsername;
                dataOutputStream.writeInt(msgBytes.length);
                dataOutputStream.write(msgBytes);
                dataOutputStream.flush();
            }

        } catch (SQLException | IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void updatePassword() {
        String sql = "select password from public.\"Users\" where \"userId\"=?";
        String sql2 = "update public.\"Users\" set password =? where \"userId\"=?";
        try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql); PreparedStatement ps2 = con.prepareStatement(sql2)) {
            String oldPassword = dataInputStream.readUTF();
            String newPassword = dataInputStream.readUTF();

            int id = getUserId();
            if (id == -1) {
                byte[] msgBytes = "can't find the user to change it's username".getBytes();
                dataOutputStream.writeInt(msgBytes.length);
                dataOutputStream.write(msgBytes);
                dataOutputStream.flush();
                return;
            }
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            String msg = "";
            if (rs.next()) {
                String password = rs.getString(1);
                if (BCrypt.checkpw(oldPassword, password)) {
                    ps.setString(1, BCrypt.hashpw(newPassword, BCrypt.gensalt()));
                    ps.setInt(2, id);
                    if (ps2.executeUpdate() > 0) {
                        msg = "Password changed successfully";
                    } else {
                        msg = "can't find the user to change it's password)";
                    }
                } else {
                    msg = "Incorrect Password";
                }
            } else {
                msg = "can't find the user to change it's password";
            }
            byte[] msgBytes = msg.getBytes();
            dataOutputStream.writeInt(msgBytes.length);
            dataOutputStream.write(msgBytes);
            dataOutputStream.flush();

        } catch (SQLException | IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void addDownloadedBook(String title) {
        int userId = getUserId();
        int bookId = getBookId(title);
        String sql = "insert into public.\"DownloadedBooks\" (\"userId\", \"bookId\") values(?, ?)";
        try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, bookId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }// </editor-fold>  

// <editor-fold defaultstate="collapsed" desc="Assist Functions Code"> 
    private int getUserId() {
        String sql = "select \"userId\" from Public.\"Users\" where username=?";
        try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }

    private int getUserId(String user) {
        String sql = "select \"userId\" from Public.\"Users\" where username=?";
        try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, user);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }

    private int getBookId(String title) {
        String sql = "select \"bookId\" from Public.\"Books\" where title=?";
        try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, title);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }

    private boolean userExists(String user) {
        String sql = "select * from public.\"Users\" where username=?";
        try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, user);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return true;
            }
        } catch (SQLException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }// </editor-fold>  

// <editor-fold defaultstate="collapsed" desc="User Table Code"> 
    private void userExists() {
        try {
            String user = dataInputStream.readUTF();
            dataOutputStream.writeBoolean(userExists(user));

        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void canUpdateUser() {
        try {
            int id = dataInputStream.readInt();
            String user = dataInputStream.readUTF();
            boolean ans = false;
            if (userExists(user)) { // if user exists
                if (id == getUserId(user)) {// if only the password is updated.
                    ans = true;
                }
            } else {
                ans = true;
            }
            dataOutputStream.writeBoolean(ans);
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void getUsers() {
        String sql = "select * from public.\"Users\" where concat(\"userId\",username) ilike ? order by \"userId\" desc";
        try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            int searchValueLength = dataInputStream.readInt();
            byte[] searchValueBytes = new byte[searchValueLength];
            dataInputStream.readFully(searchValueBytes);
            String searchValue = new String(searchValueBytes);

            ArrayList<Object[]> table = new ArrayList<>();

            ps.setString(1, "%" + searchValue + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                table.add(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3)});
            }
            objectOutputStream.writeObject(table);
            objectOutputStream.flush();
        } catch (SQLException | IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void addUser() {
        String sql = "insert into public.\"Users\"(username, password) values(?,?)";
        try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            String user = dataInputStream.readUTF();
            String password = dataInputStream.readUTF();

            ps.setString(1, user);
            ps.setString(2, BCrypt.hashpw(password, BCrypt.gensalt()));

            if (ps.executeUpdate() > 0) {
                dataOutputStream.writeBoolean(true);
            } else {
                dataOutputStream.writeBoolean(false);
            }
        } catch (SQLException | IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void updateUser() {
        String sql = "update public.\"Users\" set username =?, password =? where \"userId\"=?";
        try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            int id = dataInputStream.readInt();
            String user = dataInputStream.readUTF();
            String password = dataInputStream.readUTF();

            ps.setString(1, user);
            ps.setString(2, BCrypt.hashpw(password, BCrypt.gensalt()));
            ps.setInt(3, id);

            if (ps.executeUpdate() > 0) {
                dataOutputStream.writeBoolean(true);
            } else {
                dataOutputStream.writeBoolean(false);
            }
        } catch (SQLException | IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void deleteUser() {
        String sql = "delete from public.\"Users\" where \"userId\"=?";
        try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            int id = dataInputStream.readInt();

            ps.setInt(1, id);

            if (ps.executeUpdate() > 0) {
                dataOutputStream.writeBoolean(true);
            } else {
                dataOutputStream.writeBoolean(false);
            }
        } catch (SQLException | IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void getNextUserId() {
        int id = 0;
        String sql = "select max(\"userId\") from public.\"Users\"";
        try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                id = rs.getInt(1);
            }
            dataOutputStream.writeInt(id + 1);
        } catch (SQLException | IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }// </editor-fold>  

// <editor-fold defaultstate="collapsed" desc="Books Table Code"> 
    private void getBooks() {
        String sql = "select * from public.\"Books\" where concat(\"bookId\",title, author) ilike ? order by \"bookId\" desc";
        try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            int searchValueLength = dataInputStream.readInt();
            byte[] searchValueBytes = new byte[searchValueLength];
            dataInputStream.readFully(searchValueBytes);
            String searchValue = new String(searchValueBytes);

            ArrayList<Object[]> table = new ArrayList<>();

            ps.setString(1, "%" + searchValue + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                byte[] imageData = rs.getBytes(6);
                ImageIcon imageIcon = new ImageIcon(imageData);
                Image image = imageIcon.getImage().getScaledInstance(133, 200, Image.SCALE_SMOOTH);
                imageIcon = new ImageIcon(image);
                JLabel lebl = new JLabel("");
                lebl.setIcon(imageIcon);
                lebl.setHorizontalAlignment(JLabel.CENTER);
                table.add(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3), (String[]) rs.getArray(4).getArray(), rs.getString(5), lebl});
            }
            objectOutputStream.writeObject(table);
            objectOutputStream.flush();
        } catch (SQLException | IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void addBook() {
        String sql = "INSERT INTO public.\"Books\"(title, author, genres, book_address, cover) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            String title = objectInputStream.readUTF();
            String author = objectInputStream.readUTF();
            Object genresObject = objectInputStream.readObject();
            if (genresObject.getClass() == ArrayList.class) {
                List<String> genres = (List) genresObject;
                String address = objectInputStream.readUTF();
                int imageDataLength = objectInputStream.readInt();
                if (imageDataLength > 0) {
                    byte[] imageData = new byte[imageDataLength];
                    objectInputStream.readFully(imageData);
                    //imageData = objectInputStream.readNBytes(imageDataLength);
                    System.out.println("has image2");
                    ps.setString(1, title);
                    ps.setString(2, author);
                    Object[] genresArray = genres.toArray();
                    Array sqlArray = con.createArrayOf("text", genresArray);
                    ps.setArray(3, sqlArray);
                    ps.setString(4, address);
                    ps.setBytes(5, imageData);
                    if (ps.executeUpdate() > 0) {
                        dataOutputStream.writeBoolean(true);
                    } else {
                        dataOutputStream.writeBoolean(false);
                    }
                }
            }
        } catch (IOException | ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void updateBook() {
        String sql = "update public.\"Books\" set title=?, author=?, genres=?, book_address=?, cover=? where \"bookId\"=?";
        try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            int id = objectInputStream.readInt();
            String title = objectInputStream.readUTF();
            String author = objectInputStream.readUTF();
            Object genresObject = objectInputStream.readObject();
            if (genresObject.getClass() == ArrayList.class) {
                List<String> genres = (List) genresObject;
                String address = objectInputStream.readUTF();
                int imageDataLength = objectInputStream.readInt();
                if (imageDataLength > 0) {
                    byte[] imageData = new byte[imageDataLength];
                    objectInputStream.readFully(imageData);

                    ps.setString(1, title);
                    ps.setString(2, author);
                    Object[] genresArray = genres.toArray();
                    Array sqlArray = con.createArrayOf("text", genresArray);
                    ps.setArray(3, sqlArray);
                    ps.setString(4, address);

                    File tempFile = File.createTempFile("coverImage", ".jpg");
                    FileOutputStream fos = new FileOutputStream(tempFile);
                    fos.write(imageData);
                    FileInputStream fis = new FileInputStream(tempFile);

                    ps.setBinaryStream(5, fis, (int) tempFile.length());
                    tempFile.delete();

                    ps.setInt(6, id);

                    if (ps.executeUpdate() > 0) {
                        System.out.println("updated succesfully");
                        dataOutputStream.writeBoolean(true);
                    } else {
                        dataOutputStream.writeBoolean(false);
                    }
                }
            }
        } catch (IOException | ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void deleteBook() {
        String sql = "delete from public.\"Books\" where \"bookId\"=?";
        try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            int id = dataInputStream.readInt();
            ps.setInt(1, id);
            if (ps.executeUpdate() > 0) {
                dataOutputStream.writeBoolean(true);
            } else {
                dataOutputStream.writeBoolean(false);
            }
        } catch (IOException | SQLException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void getNextBookId() {
        String sql = "select max(\"bookId\") from public.\"Books\"";
        int id = 0;
        try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                id = rs.getInt(1);
            }
            dataOutputStream.writeInt(id + 1);
        } catch (SQLException | IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void bookExists() {
        String sql = "select * from public.\"Books\" where title=?";
        try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            String title = dataInputStream.readUTF();
            ps.setString(1, title);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                dataOutputStream.writeBoolean(true);
            } else {
                dataOutputStream.writeBoolean(false);
            }

        } catch (IOException | SQLException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
// </editor-fold>  

    // <editor-fold defaultstate="collapsed" desc="Downloaded Books Table Code">
    private void getDownloadedBooks() {

        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            int numOfFilters = objectInputStream.readInt();
            Object filtersObject = objectInputStream.readObject();
            if (filtersObject.getClass() == ArrayList.class) {
                List<String> filters = (List<String>) filtersObject;

                int searchValueLength = objectInputStream.readInt();
                byte[] searchValueBytes = new byte[searchValueLength];
                objectInputStream.readFully(searchValueBytes);
                String searchValue = new String(searchValueBytes);

                String sql = "SELECT Public.\"Users\".\"username\", public.\"Books\".title, public.\"Books\".author "
                        + "FROM Public.\"DownloadedBooks\" "
                        + "JOIN Public.\"Users\" ON Public.\"Users\".\"userId\" = public.\"DownloadedBooks\".\"userId\" "
                        + "JOIN public.\"Books\" ON public.\"Books\".\"bookId\" = public.\"DownloadedBooks\".\"bookId\" "
                        + "WHERE concat (";
                if (filters.get(0).equals("")) {
                    sql += "''";
                } else {
                    sql += filters.get(0);
                }
                for (int i = 1; i <= numOfFilters - 1; i++) {
                    sql += ", " + filters.get(i);
                }
                sql += ") ILIKE ? ORDER BY Public.\"Users\".\"username\"";

                Connection con = dataSource.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setString(1, "%" + searchValue + "%");
                ResultSet rs = ps.executeQuery();
                ArrayList<Object[]> table = new ArrayList<>();
                while (rs.next()) {
                    table.add(new Object[]{rs.getString(1), rs.getString(2), rs.getString(3)});
                }
                objectOutputStream.writeObject(table);
                objectOutputStream.flush();
            }
        } catch (SQLException | IOException | ClassNotFoundException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    // </editor-fold>

    private void getPassword() {
        String sql = "select password from public.\"Users\" where username=?";
        try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            String user = dataInputStream.readUTF();

            ps.setString(1, user);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                dataOutputStream.writeUTF(rs.getString(1));
            } else {
                dataOutputStream.writeUTF("");
            }

        } catch (IOException | SQLException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    /*
        datainputstream:
            second data = action
               rest if the data = depends on the action
                    get: searchvalue.length (int), searchvalue(string) 
                    getDownloads: nothing
                    username: newusername.length(int), newUsername(String)
                    password: oldPassword.length(int), oldPassword(String), newPassword.length(int), newPassword(String)
                    download: bookTitleLength(int), bookTitle(String)
                    
        return to client:
            get: objectoutputstream = table (ArrayList<Object[]>)
            getDownloads: objectoutputstream = table (ArrayList<Object[]>) 
            username: dataOutputStream = message (string)
            password: dataOutputStream = message (string)
            download: dataOutputStream = fileNameBytes.lenght, fileNameBytes, fileBytes.length, fileBytes
     */
}
