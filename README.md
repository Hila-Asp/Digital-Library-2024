# Digital Library
An "online shop" where users can download digital books (.epub files).

# To Make This Code Work

- Add the jars from the jars folder to the project.
- Update db/MyConnection.java to match your database's data.
- Run Server.java</br>
  It will create all 3 databases needed for the project, it will add the admin user to the "Users" table and it will give you a default admin password.
- Login with the default password then change it.
- If you want to create the databases manually:
  - Create 3 tables (I used Postgres). (The scripts are in Database_Create_Script.txt)
    - Users:
        -  userId (Primary key) (serial)
        -  username (text)
        -  password (text)
    - Books:
        - bookId (Primary key) (serial)
        - title (text)
        - author (text)
        - genres (text[])
        - book_address (text)
        - cover (bytea)
    - DownloadedBooks:
        - transactionId (Primary key) (serial)
        - userId (integer)
        - bookId (integer)
        - dateDownloaded (date)
  - Add manually the admin password (it's important to insert the hashed password to the database)
- Add books
- To read the books you need to have an epub viewer.
