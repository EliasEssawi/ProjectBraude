This is a college project built using the **OCSF (Open Chat Server Framework)**.  
The application manages parking spaces: requesting, reserving, and updating parking spot statuses.

**Note:** The project is still in development and not yet fully complete.

 Requirements

To run the client and server, make sure you have:

- **JDK 24** (Java Development Kit)
- **JavaFX SDK 24**
- **OCSF framework** (either as `.jar` or source file)
- **MySQL Server**

---
Database Setup 
`ProjectServer/src/server/mysqlConnection.java`

You need to update the MySQL connection string in this file.

Replace the following line:

```java
Connection conn = DriverManager.getConnection(
    "jdbc:mysql://localhost:3306/parking?useSSL=false&serverTimezone=Asia/Jerusalem",
    "yourUsernameHere", "yourPasswordHere");
Where is written "yourUsernameHere", "yourPasswordHere" you need to write the file of the SQL name and the Password you chose for it in order to connect!
Note: In this connection string, the database name is parking:
"jdbc:mysql://localhost:3306/parking..."

Make sure this database exists, or update the name if you use a different one.
