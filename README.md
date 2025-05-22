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

You need to update the MySQL connection string in the file:
Connection conn = DriverManager.getConnection(
    "jdbc:mysql://localhost:3306/parking?useSSL=false&serverTimezone=Asia/Jerusalem",
    "yourUsernameHere", "yourPasswordHere"); 
also notice that in this line my schema name was parking! --> "jdbc:mysql://localhost:3306/parking...
