/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ClientSide;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author Hila
 */
public class Client {

    Socket socket;
    private static ObjectInputStream objectInputStream;

    public Client(String username) {
        try {
            socket = new Socket("localhost", 1234);
            objectInputStream = new ObjectInputStream(socket.getInputStream());
            DataOutputStream sendUsername = new DataOutputStream(socket.getOutputStream());
            byte[] usernameBytes = username.getBytes();
            sendUsername.writeInt(usernameBytes.length);
            sendUsername.write(usernameBytes);
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String downloadBook(String title) {
        try {
            DataOutputStream sendAction = new DataOutputStream(socket.getOutputStream());
            byte[] actionBytes = "download".getBytes();
            sendAction.writeInt(actionBytes.length);
            sendAction.write(actionBytes);

            DataOutputStream bookTitle = new DataOutputStream(socket.getOutputStream());
            byte[] titleBytes = title.getBytes();
            bookTitle.writeInt(titleBytes.length);
            bookTitle.write(titleBytes);

            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            int fileNameLength = dataInputStream.readInt();

            if (fileNameLength > 0) {
                byte[] fileNameBytes = new byte[fileNameLength];
                dataInputStream.readFully(fileNameBytes, 0, fileNameBytes.length);
                String fileName = new String(fileNameBytes);
                int fileContentLength = dataInputStream.readInt();

                if (fileContentLength > 0) {
                    byte[] fileContentBytes = new byte[fileContentLength];
                    dataInputStream.readFully(fileContentBytes, 0, fileContentBytes.length);

                    String userHome = System.getProperty("user.home");
                    File downloadsDir = Paths.get(userHome, "Downloads").toFile();

                    // Create the file with the full path in the Downloads folder
                    File fileToDownload = new File(downloadsDir, fileName);
                    // Create a stream to write data to the file.
                    FileOutputStream fileOutputStream = new FileOutputStream(fileToDownload);
                    // Write the actual file data to the file.
                    fileOutputStream.write(fileContentBytes);
                    // Close the stream.
                    fileOutputStream.close();
                    return fileToDownload.getAbsolutePath();
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public ArrayList<Object[]> getBooks(String searchValue) {
        try {
            DataOutputStream sendAction = new DataOutputStream(socket.getOutputStream());
            byte[] actionBytes = "getUserBooks".getBytes();
            sendAction.writeInt(actionBytes.length);
            sendAction.write(actionBytes);

            DataOutputStream sendSearch = new DataOutputStream(socket.getOutputStream());
            byte[] searchBytes = searchValue.getBytes();
            sendSearch.writeInt(searchBytes.length);
            sendSearch.write(searchBytes);

            //ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            Object object = objectInputStream.readObject();
            if (object.getClass() == ArrayList.class) {
                ArrayList<Object[]> table = (ArrayList<Object[]>) object;
                return table;
            }
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public ArrayList<Object[]> getDownloadedBooks() {
        try {
            DataOutputStream sendAction = new DataOutputStream(socket.getOutputStream());
            byte[] actionBytes = "getUserDownloads".getBytes();
            sendAction.writeInt(actionBytes.length);
            sendAction.write(actionBytes);

            //ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            Object object = objectInputStream.readObject();
            if (object.getClass() == ArrayList.class) {
                ArrayList<Object[]> table = (ArrayList<Object[]>) object;
                return table;
            }
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public boolean updateUsername(String newUsername) {
        try {
            DataOutputStream sendAction = new DataOutputStream(socket.getOutputStream());
            byte[] actionBytes = "username".getBytes();
            sendAction.writeInt(actionBytes.length);
            sendAction.write(actionBytes);

            DataOutputStream sendUsername = new DataOutputStream(socket.getOutputStream());
            sendUsername.writeUTF(newUsername); // utf so that the numbers in the username will be ok

            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            int msgLength = dataInputStream.readInt();
            if (msgLength > 0) {
                byte[] msgBytes = new byte[msgLength];
                dataInputStream.readFully(msgBytes);
                String msg = new String(msgBytes);
                JOptionPane.showMessageDialog(null, msg);
                if (msg.equals("Username changed successfully")) {
                    return true;
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public void updatePassword(String oldPassword, String newPassword) {
        try {
            DataOutputStream sendAction = new DataOutputStream(socket.getOutputStream());
            byte[] actionBytes = "password".getBytes();
            sendAction.writeInt(actionBytes.length);
            sendAction.write(actionBytes);

            DataOutputStream sendOldPassword = new DataOutputStream(socket.getOutputStream());
            sendOldPassword.writeUTF(oldPassword);

            DataOutputStream sendNewPassword = new DataOutputStream(socket.getOutputStream());
            sendNewPassword.writeUTF(newPassword);

            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            int msgLength = dataInputStream.readInt();
            if (msgLength > 0) {
                byte[] msgBytes = new byte[msgLength];
                dataInputStream.readFully(msgBytes);
                String msg = new String(msgBytes);
                JOptionPane.showMessageDialog(null, msg);
            }
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
// <editor-fold defaultstate="collapsed" desc="Login\Register Code">
    public boolean userExists(String username) {
        try {
            DataOutputStream sendAction = new DataOutputStream(socket.getOutputStream());
            byte[] actionBytes = "userExists".getBytes();
            sendAction.writeInt(actionBytes.length);
            sendAction.write(actionBytes);

            DataOutputStream sendData = new DataOutputStream(socket.getOutputStream());
            sendData.writeUTF(username);

            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            return dataInputStream.readBoolean();
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public String getPassword(String username) {
try {
            DataOutputStream sendAction = new DataOutputStream(socket.getOutputStream());
            byte[] actionBytes = "getPassword".getBytes();
            sendAction.writeInt(actionBytes.length);
            sendAction.write(actionBytes);

            DataOutputStream sendData = new DataOutputStream(socket.getOutputStream());
            sendData.writeUTF(username);

            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            return dataInputStream.readUTF();
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }
    
    public boolean addUser(String username, String password) {
        try {
            DataOutputStream sendAction = new DataOutputStream(socket.getOutputStream());
            byte[] actionBytes = "addUser".getBytes();
            sendAction.writeInt(actionBytes.length);
            sendAction.write(actionBytes);

            DataOutputStream sendData = new DataOutputStream(socket.getOutputStream());
            sendData.writeUTF(username);
            sendData.writeUTF(password);

            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            boolean successful = dataInputStream.readBoolean();

            if (!successful) {
                JOptionPane.showMessageDialog(null, "Couldn't register");
                return false;
            } 

        } catch (IOException ex) {
            Logger.getLogger(Admin.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }// </editor-fold>

    public void close() {
        try {
            socket.close();
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
