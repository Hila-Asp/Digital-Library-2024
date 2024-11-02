/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ClientServer;

import db.MyConnection;
import java.awt.Image;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.DatabaseMetaData;

/**
 *
 * @author Hila
 */
public class Server {

    private static final Connection con = MyConnection.getConnection();
    private static PreparedStatement ps;
    private static String username;
    private static DataInputStream dataInputStream;
    private static DataOutputStream dataOutputStream;
    private static ObjectOutputStream objectOutputStream;
    private static Socket socket;

    private static boolean doesTableExist(String tableName) throws SQLException {
        DatabaseMetaData metaData = con.getMetaData();
        try (ResultSet resultSet = metaData.getTables(null, null, tableName, new String[]{"TABLE"})) {
            return resultSet.next();
        }
    }

    public static void main(String[] args) {
        try {
            ResultSet rs;
            if (!doesTableExist("gsers")) { //checks if "Users" table exists
                String sql = "CREATE TABLE public.\"gsers\" ("
                        + "\"userId\" serial NOT NULL, "
                        + "username text NOT NULL, "
                        + "password text NOT NULL, "
                        + "PRIMARY KEY (\"userId\"))";
                String sql2 = "Insert Into public.\"gsers\" (username, password) values (?, ?)";
                ps = con.prepareStatement(sql);
                ps.executeUpdate();
                if (doesTableExist("gsers")) {
                    System.out.println("Users table created");
                } else {
                    System.out.println("Couldn't create Users table");
                }
                ps = con.prepareStatement(sql2);
                ps.setString(1, "ADMIN");
                ps.setString(2, "Admin");
                if (ps.executeUpdate() > 0) {
                    System.out.println("Admin initialization successful.\n Password is: Admin");
                } else {
                    System.out.println("Couldn't initialize the admin.");
                }
            }

            if (!doesTableExist("gooks")) {
                String sql3 = "CREATE TABLE public.\"gooks\" ("
                        + "\"bookId\" serial NOT NULL, "
                        + "title text NOT NULL, "
                        + "author text NOT NULL, "
                        + "genres text[] NOT NULL, "
                        + "    book_address text NOT NULL, "
                        + "    cover bytea NOT NULL, "
                        + "    PRIMARY KEY (\"bookId\"))";
                ps = con.prepareStatement(sql3);
                ps.executeUpdate();
                if (doesTableExist("gooks")) {
                    System.out.println("Books table created");
                } else {
                    System.out.println("couldn't create books table");
                }
            }
            if (!doesTableExist("Downloadedgooks")) {
                String sql4 = "CREATE TABLE public.\"Downloadedgooks\" ("
                        + "\"transactionId\" serial NOT NULL, "
                        + "\"userId\" integer NOT NULL, "
                        + "\"bookId\" integer NOT NULL, "
                        + "\"dateDownloaded\" date NOT NULL, "
                        + "    PRIMARY KEY (\"transactionId\"))";
                ps = con.prepareStatement(sql4);
                ps.executeUpdate();
                if (doesTableExist("Downloadedgooks")) {
                    System.out.println("Downloaded Books table created");
                } else {
                    System.out.println("Couldn't create downloaded books table");
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
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
        try {
            ServerSocket serverSocket = new ServerSocket(1234);
            System.out.println("Server running");

            while (true) {
                System.out.println("Waiting for client...");
                socket = serverSocket.accept();
                System.out.println("Client connected.");

                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                objectOutputStream = new ObjectOutputStream(socket.getOutputStream());

                try {
                    int usernameLength = dataInputStream.readInt();
                    byte[] usernameBytes = new byte[usernameLength];
                    dataInputStream.readFully(usernameBytes);
                    username = new String(usernameBytes);

                    String action = "";
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
                    System.out.println("Client disconnected. ");
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }
// <editor-fold defaultstate="collapsed" desc="Mange User Form Code"> 

    private static void getUserBooks() {
        try {
            int searchValueLength = dataInputStream.readInt();
            byte[] searchValueBytes = new byte[searchValueLength];
            dataInputStream.readFully(searchValueBytes);
            String searchValue = new String(searchValueBytes);

            ArrayList<Object[]> table = new ArrayList<>();
            String sql = "select title, author, genres, cover from public.\"Books\" where concat(title, author) ilike ? order by title";

            ps = con.prepareStatement(sql);
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
                table.add(new Object[]{rs.getString(1), rs.getString(2), (String[]) rs.getArray(3).getArray(), lebl, "Download"});
            }
            objectOutputStream.writeObject(table);
            objectOutputStream.flush();
        } catch (SQLException | IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void getDownloads() {
        try {
            int id = getUserId();
            if (id == -1) {
                return;
            }
            String sql = "SELECT public.\"Books\".title, public.\"Books\".author, public.\"Books\".genres, public.\"Books\".cover "
                    + "FROM Public.\"DownloadedBooks\" "
                    + "JOIN public.\"Books\" ON public.\"Books\".\"bookId\" = public.\"DownloadedBooks\".\"bookId\" "
                    + "WHERE public.\"DownloadedBooks\".\"userId\"=?"
                    //+ " AND concat (title, author) LIKE ?"
                    + " ORDER BY Public.\"Books\".title";
            ArrayList<Object[]> table = new ArrayList<>();

            ps = con.prepareStatement(sql);
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
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private static void download() {
        try {
            int bookTitleLength = dataInputStream.readInt();
            if (bookTitleLength > 0) {
                byte[] bookTitleBytes = new byte[bookTitleLength];
                dataInputStream.readFully(bookTitleBytes);
                String bookTitle = new String(bookTitleBytes);

                String sql = "Select book_address from public.\"Books\" where title=?";
                ps = con.prepareStatement(sql);
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
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void updateUsername() {
        try {
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
            ps = con.prepareStatement("update public.\"Users\" set username =? where \"userId\"=?");
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
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static void updatePassword() {
        try {
            String oldPassword = dataInputStream.readUTF();
            String newPassword = dataInputStream.readUTF();

            String sql1 = "select password from public.\"Users\" where \"userId\"=?";
            String sql2 = "update public.\"Users\" set password =? where \"userId\"=?";
            int id = getUserId();
            if (id == -1) {
                byte[] msgBytes = "can't find the user to change it's username".getBytes();
                dataOutputStream.writeInt(msgBytes.length);
                dataOutputStream.write(msgBytes);
                dataOutputStream.flush();
                return;
            }
            ps = con.prepareStatement(sql1);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            String msg = "";
            if (rs.next()) {
                String password = rs.getString(1);
                if (BCrypt.checkpw(oldPassword, password)) {
                    ps = con.prepareStatement(sql2);
                    ps.setString(1, BCrypt.hashpw(newPassword, BCrypt.gensalt()));
                    ps.setInt(2, id);
                    if (ps.executeUpdate() > 0) {
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
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void addDownloadedBook(String title) {
        int userId = getUserId();
        int bookId = getBookId(title);
        String sql = "insert into public.\"DownloadedBooks\" (\"userId\", \"bookId\") values(?, ?)";
        try {
            ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setInt(2, bookId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }// </editor-fold>  

// <editor-fold defaultstate="collapsed" desc="Assist Functions Code"> 
    public static int getUserId() {
        String sql = "select \"userId\" from Public.\"Users\" where username=?";
        try {
            ps = con.prepareStatement(sql);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }

    public static int getUserId(String user) {
        String sql = "select \"userId\" from Public.\"Users\" where username=?";
        try {
            ps = con.prepareStatement(sql);
            ps.setString(1, user);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }

    public static int getBookId(String title) {
        String sql = "select \"bookId\" from Public.\"Books\" where title=?";
        try {
            ps = con.prepareStatement(sql);
            ps.setString(1, title);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }

    public static boolean userExists(String user) {
        try {
            ps = con.prepareStatement("select * from public.\"Users\" where username=?");
            ps.setString(1, user);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return true;
            }
        } catch (SQLException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }// </editor-fold>  

// <editor-fold defaultstate="collapsed" desc="User Table Code"> 
    public static void userExists() {
        try {
            String user = dataInputStream.readUTF();
            dataOutputStream.writeBoolean(userExists(user));

        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void canUpdateUser() {
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
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void getUsers() {
        try {
            int searchValueLength = dataInputStream.readInt();
            byte[] searchValueBytes = new byte[searchValueLength];
            dataInputStream.readFully(searchValueBytes);
            String searchValue = new String(searchValueBytes);

            ArrayList<Object[]> table = new ArrayList<>();
            String sql = "select * from public.\"Users\" where concat(\"userId\",username) ilike ? order by \"userId\" desc";

            ps = con.prepareStatement(sql);
            ps.setString(1, "%" + searchValue + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                table.add(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3)});
            }
            objectOutputStream.writeObject(table);
            objectOutputStream.flush();
        } catch (SQLException | IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void addUser() {
        try {
            String user = dataInputStream.readUTF();
            String password = dataInputStream.readUTF();

            ps = con.prepareStatement("insert into public.\"Users\"(username, password) values(?,?)");
            ps.setString(1, user);
            ps.setString(2, BCrypt.hashpw(password, BCrypt.gensalt()));

            if (ps.executeUpdate() > 0) {
                dataOutputStream.writeBoolean(true);
            } else {
                dataOutputStream.writeBoolean(false);
            }
        } catch (SQLException | IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void updateUser() {
        try {
            int id = dataInputStream.readInt();
            String user = dataInputStream.readUTF();
            String password = dataInputStream.readUTF();

            ps = con.prepareStatement("update public.\"Users\" set username =?, password =? where \"userId\"=?");
            ps.setString(1, user);
            ps.setString(2, BCrypt.hashpw(password, BCrypt.gensalt()));
            ps.setInt(3, id);

            if (ps.executeUpdate() > 0) {
                dataOutputStream.writeBoolean(true);
            } else {
                dataOutputStream.writeBoolean(false);
            }
        } catch (SQLException | IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void deleteUser() {
        try {
            int id = dataInputStream.readInt();

            ps = con.prepareStatement("delete from public.\"Users\" where \"userId\"=?");
            ps.setInt(1, id);

            if (ps.executeUpdate() > 0) {
                dataOutputStream.writeBoolean(true);
            } else {
                dataOutputStream.writeBoolean(false);
            }
        } catch (SQLException | IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void getNextUserId() {
        int id = 0;
        try {
            ps = con.prepareStatement("select max(\"userId\") from public.\"Users\"");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                id = rs.getInt(1);
            }
            dataOutputStream.writeInt(id + 1);
        } catch (SQLException | IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }// </editor-fold>  

// <editor-fold defaultstate="collapsed" desc="Books Table Code"> 
    public static void getBooks() {
        try {
            int searchValueLength = dataInputStream.readInt();
            byte[] searchValueBytes = new byte[searchValueLength];
            dataInputStream.readFully(searchValueBytes);
            String searchValue = new String(searchValueBytes);

            ArrayList<Object[]> table = new ArrayList<>();

            String sql = "select * from public.\"Books\" where concat(\"bookId\",title, author) ilike ? order by \"bookId\" desc";
            ps = con.prepareStatement(sql);
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
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void addBook() {
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            String title = objectInputStream.readUTF();
            String author = objectInputStream.readUTF();
            Object genresObject = objectInputStream.readObject();
            if (genresObject.getClass() == List.class) {
                List<String> genres = (List) genresObject;
                String address = objectInputStream.readUTF();
                int imageDataLength = objectInputStream.readInt();
                if (imageDataLength > 0) {
                    byte[] imageData = new byte[imageDataLength];
                    objectInputStream.readFully(imageData);
                    ps = con.prepareStatement("insert into public.\"Books\"(title, author, genres, book_address, cover) values(?,?,?,?,?)");
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
                    fis.close();

                    if (ps.executeUpdate() > 0) {
                        dataOutputStream.writeBoolean(true);
                    } else {
                        dataOutputStream.writeBoolean(false);
                    }
                }
            }
        } catch (IOException | ClassNotFoundException | SQLException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void updateBook() {
        try {
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

                    ps = con.prepareStatement("update public.\"Books\" set title=?, author=?, genres=?, book_address=?, cover=? where \"bookId\"=?");
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
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void deleteBook() {
        try {
            int id = dataInputStream.readInt();
            ps = con.prepareStatement("delete from public.\"Books\" where \"bookId\"=?");
            ps.setInt(1, id);
            if (ps.executeUpdate() > 0) {
                dataOutputStream.writeBoolean(true);
            } else {
                dataOutputStream.writeBoolean(false);
            }
        } catch (IOException | SQLException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void getNextBookId() {
        int id = 0;
        try {
            ps = con.prepareStatement("select max(\"bookId\") from public.\"Books\"");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                id = rs.getInt(1);
            }
            dataOutputStream.writeInt(id + 1);
        } catch (SQLException | IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void bookExists() {
        try {
            String title = dataInputStream.readUTF();
            dataOutputStream.writeBoolean(userExists(title));

            ps = con.prepareStatement("select * from public.\"Users\" where username=?");
            ps.setString(1, title);
            if (ps.executeUpdate() > 0) {
                dataOutputStream.writeBoolean(true);
            } else {
                dataOutputStream.writeBoolean(false);
            }

        } catch (IOException | SQLException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
// </editor-fold>  

    // <editor-fold defaultstate="collapsed" desc="Downloaded Books Table Code">
    public static void getDownloadedBooks() {

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

                ps = con.prepareStatement(sql);
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
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    // </editor-fold>

    public static void getPassword() {
        try {
            String user = dataInputStream.readUTF();
            String sql = "select password from public.\"Users\" where username=?";
            ps = con.prepareStatement(sql);
            ps.setString(1, user);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                dataOutputStream.writeUTF(rs.getString(1));
            } else {
                dataOutputStream.writeUTF("");
            }

        } catch (IOException | SQLException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
