/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ClientServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author Hila
 */
public class Admin {

    Socket socket;
    private static ObjectInputStream objectInputStream;

    public Admin() {
        try {
            socket = new Socket("localhost", 1234);
            objectInputStream = new ObjectInputStream(socket.getInputStream());
            DataOutputStream sendUsername = new DataOutputStream(socket.getOutputStream());
            byte[] usernameBytes = "ADMIN".getBytes();
            sendUsername.writeInt(usernameBytes.length);
            sendUsername.write(usernameBytes);
        } catch (IOException ex) {
            Logger.getLogger(Admin.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // <editor-fold defaultstate="collapsed" desc="Users Code"> 
    public void addUser(String username, String password) {
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

            if (successful) {
                JOptionPane.showMessageDialog(null, "User added successfully");
            } else {
                JOptionPane.showMessageDialog(null, "Couldn't add user");
            }

        } catch (IOException ex) {
            Logger.getLogger(Admin.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void updateUser(int id, String username, String password) {
        try {
            DataOutputStream sendAction = new DataOutputStream(socket.getOutputStream());
            byte[] actionBytes = "updateUser".getBytes();
            sendAction.writeInt(actionBytes.length);
            sendAction.write(actionBytes);

            DataOutputStream sendData = new DataOutputStream(socket.getOutputStream());
            sendData.writeInt(id);
            sendData.writeUTF(username);
            sendData.writeUTF(password);

            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            boolean successful = dataInputStream.readBoolean();

            if (successful) {
                JOptionPane.showMessageDialog(null, "User updated successfully");
            } else {
                JOptionPane.showMessageDialog(null, "Couldn't update user");
            }

        } catch (IOException ex) {
            Logger.getLogger(Admin.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void deleteUser(int id) {
        try {
            int yesOrNo = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete this user?", "Student delete", JOptionPane.OK_CANCEL_OPTION);
            if (yesOrNo == JOptionPane.OK_OPTION) {
                DataOutputStream sendAction = new DataOutputStream(socket.getOutputStream());
                byte[] actionBytes = "deleteUser".getBytes();
                sendAction.writeInt(actionBytes.length);
                sendAction.write(actionBytes);

                DataOutputStream sendId = new DataOutputStream(socket.getOutputStream());
                sendId.writeInt(id);

                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                boolean successful = dataInputStream.readBoolean();

                if (successful) {
                    JOptionPane.showMessageDialog(null, "User deleted successfully");
                } else {
                    JOptionPane.showMessageDialog(null, "Couldn't delete user");
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Admin.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public ArrayList<Object[]> getUsers(String searchValue) {
        try {
            DataOutputStream sendAction = new DataOutputStream(socket.getOutputStream());
            byte[] actionBytes = "getUsers".getBytes();
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
        return new ArrayList<>();
    }

    public boolean userExists(String user) {
        try {
            DataOutputStream sendAction = new DataOutputStream(socket.getOutputStream());
            byte[] actionBytes = "userExists".getBytes();
            sendAction.writeInt(actionBytes.length);
            sendAction.write(actionBytes);

            DataOutputStream sendData = new DataOutputStream(socket.getOutputStream());
            sendData.writeUTF(user);

            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            return dataInputStream.readBoolean();

        } catch (IOException ex) {
            Logger.getLogger(Admin.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public boolean canUpdateUser(int id, String user) {
        try {
            DataOutputStream sendAction = new DataOutputStream(socket.getOutputStream());
            byte[] actionBytes = "canUpdateUser".getBytes();
            sendAction.writeInt(actionBytes.length);
            sendAction.write(actionBytes);

            DataOutputStream sendData = new DataOutputStream(socket.getOutputStream());
            sendData.writeInt(id);
            sendData.writeUTF(user);

            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            return dataInputStream.readBoolean();

        } catch (IOException ex) {
            Logger.getLogger(Admin.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public int getNextUserId() {
        try {
            DataOutputStream sendAction = new DataOutputStream(socket.getOutputStream());
            byte[] actionBytes = "getNextId".getBytes();
            sendAction.writeInt(actionBytes.length);
            sendAction.write(actionBytes);

            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            return dataInputStream.readInt();
        } catch (IOException ex) {
            Logger.getLogger(Admin.class.getName()).log(Level.SEVERE, null, ex);
        }
        return 0;
    }// </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Books Code"> 
    public void addBook(String title, String author, List<String> genres, String address, byte[] imageData) {
        try {
            DataOutputStream sendAction = new DataOutputStream(socket.getOutputStream());
            byte[] actionBytes = "addBook".getBytes();
            sendAction.writeInt(actionBytes.length);
            sendAction.write(actionBytes);

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.writeUTF(title);
            objectOutputStream.writeUTF(author);
            objectOutputStream.writeObject(genres);
            objectOutputStream.writeUTF(address);
            objectOutputStream.writeInt(imageData.length);
            objectOutputStream.write(imageData);

            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            boolean successful = dataInputStream.readBoolean();

            if (successful) {
                JOptionPane.showMessageDialog(null, "Book added successfully");
            } else {
                JOptionPane.showMessageDialog(null, "Couldn't add Book");
            }

        } catch (IOException ex) {
            Logger.getLogger(Admin.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void updateBook(int id, String title, String author, List<String> genres, String address, byte[] imageData) {
        try {
            DataOutputStream sendAction = new DataOutputStream(socket.getOutputStream());
            byte[] actionBytes = "updateBook".getBytes();
            sendAction.writeInt(actionBytes.length);
            sendAction.write(actionBytes);

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.writeInt(id);
            objectOutputStream.writeUTF(title);
            objectOutputStream.writeUTF(author);
            objectOutputStream.writeObject(genres);
            objectOutputStream.writeUTF(address);
            objectOutputStream.writeInt(imageData.length);
            objectOutputStream.write(imageData);
            objectOutputStream.flush();

            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            boolean successful = dataInputStream.readBoolean();

            if (successful) {
                JOptionPane.showMessageDialog(null, "Book updated successfully");
            } else {
                JOptionPane.showMessageDialog(null, "Couldn't update the book");
            }

        } catch (IOException ex) {
            Logger.getLogger(Admin.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void deleteBook(int id) {
        int yesOrNo = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete this Book?", "Student delete", JOptionPane.OK_CANCEL_OPTION);
        if (yesOrNo == JOptionPane.OK_OPTION) {
            try {
                DataOutputStream sendAction = new DataOutputStream(socket.getOutputStream());
                byte[] actionBytes = "deleteBook".getBytes();
                sendAction.writeInt(actionBytes.length);
                sendAction.write(actionBytes);

                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataOutputStream.writeInt(id);

                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                boolean successful = dataInputStream.readBoolean();

                if (successful) {
                    JOptionPane.showMessageDialog(null, "Book deleted successfully");
                } else {
                    JOptionPane.showMessageDialog(null, "Couldn't delete the book");
                }
            } catch (IOException ex) {
                Logger.getLogger(Admin.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public ArrayList<Object[]> getBooks(String searchValue) {
        try {
            DataOutputStream sendAction = new DataOutputStream(socket.getOutputStream());
            byte[] actionBytes = "getBooks".getBytes();
            sendAction.writeInt(actionBytes.length);
            sendAction.write(actionBytes);

            DataOutputStream sendSearch = new DataOutputStream(socket.getOutputStream());
            byte[] searchBytes = searchValue.getBytes();
            sendSearch.writeInt(searchBytes.length);
            sendSearch.write(searchBytes);

            Object object = objectInputStream.readObject();
            if (object.getClass() == ArrayList.class) {
                ArrayList<Object[]> table = (ArrayList<Object[]>) object;
                return table;
            }
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new ArrayList<>();
    }

    public int getNextBookId() {
        try {
            DataOutputStream sendAction = new DataOutputStream(socket.getOutputStream());
            byte[] actionBytes = "getNextBookId".getBytes();
            sendAction.writeInt(actionBytes.length);
            sendAction.write(actionBytes);

            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            return dataInputStream.readInt();
        } catch (IOException ex) {
            Logger.getLogger(Admin.class.getName()).log(Level.SEVERE, null, ex);
        }
        return 0;
    }

    public boolean bookExists(String title) {
        try {
            DataOutputStream sendAction = new DataOutputStream(socket.getOutputStream());
            byte[] actionBytes = "bookExists".getBytes();
            sendAction.writeInt(actionBytes.length);
            sendAction.write(actionBytes);

            DataOutputStream sendData = new DataOutputStream(socket.getOutputStream());
            sendData.writeUTF(title);

            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            return dataInputStream.readBoolean();

        } catch (IOException ex) {
            Logger.getLogger(Admin.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }
    // </editor-fold>  

    // <editor-fold defaultstate="collapsed" desc="Downloaded table Code">
    public ArrayList<Object[]> getDownloadedBooks(int numOfFilters, List<String> filters, String searchValue) {
        try {
            DataOutputStream sendAction = new DataOutputStream(socket.getOutputStream());
            byte[] actionBytes = "getDownloadedBooks".getBytes();
            sendAction.writeInt(actionBytes.length);
            sendAction.write(actionBytes);

            ObjectOutputStream sendData = new ObjectOutputStream(socket.getOutputStream());
            sendData.writeInt(numOfFilters);
            sendData.writeObject(filters);
            byte[] searchBytes = searchValue.getBytes();
            sendData.writeInt(searchBytes.length);
            sendData.write(searchBytes);
            sendData.flush();

            Object object = objectInputStream.readObject();
            if (object.getClass() == ArrayList.class) {
                ArrayList<Object[]> table = (ArrayList<Object[]>) object;
                return table;
            }
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new ArrayList<>();
    }
    // </editor-fold>

    public void close() {
        try {
            socket.close();
        } catch (IOException ex) {
            Logger.getLogger(Admin.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
