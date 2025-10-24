// All imports stay unchanged
package server;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDate;/*@author Amit_Regev*/
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import gui.ServerMessageFrameController;
import gui.ServerPortFrameController;

/**
 * Singleton class for managing a connection pool to a MySQL database.
 * This class provides connection pooling to efficiently manage and reuse database connections
 * across the server application. It initializes a fixed number of connections and allows acquiring
 * and returning connections from the pool.
 * 
 * <p>Database: bpark</p>
 * <p>User: root</p>
 * 
 * @author Bahaa
 */
public class mysqlConnection {

    /** Singleton instance of the connection pool manager. */
    private static mysqlConnection conn;

    /** Database URL for MySQL connection. */
    private static final String DB_url = "jdbc:mysql://localhost/bpark?useSSL=false&serverTimezone=Asia/Jerusalem";

    /** Database URL for MySQL connection. if schema doesn't exist */
    private static final String DB_URL_WITHOUT_SCHEMA = "jdbc:mysql://localhost/?useSSL=false&serverTimezone=Asia/Jerusalem";

    /** Database username. */
    private static final String DB_USER = "root";

    /** Database password. */
    private static final String DB_Password = "Ee030620";

    /** Maximum number of connections allowed in the pool. */
    private static final int Max_Pool_size = 5;

    /** Initial number of connections to create in the pool. */
    private static final int Initial_Pool_size = 2;

    /** Thread-safe queue holding the connection pool. */
    private BlockingQueue<Connection> connectionPool;

    /**
     * Private constructor that loads the MySQL JDBC driver and initializes the connection pool.
     */
    private mysqlConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("MySQL JDBC Driver not found: " + e.getMessage());
            throw new RuntimeException("Failed to load JDBC driver");
        }
        createAllTables();
        connectionPool = new ArrayBlockingQueue<>(Max_Pool_size);
        initializePool();
    }

    /**
     * Initializes the connection pool with {@code Initial_Pool_size} connections.
     */
    private void initializePool() {
        System.out.println("Initializing database connection pool with " + Initial_Pool_size + " connections...");
        for (int i = 0; i < Initial_Pool_size; i++) {
            try {
                Connection connection = DriverManager.getConnection(DB_url, DB_USER, DB_Password);
                connectionPool.offer(connection);
                System.out.println(" > Connection " + (i + 1) + " created and added to pool.");
            } catch (SQLException e) {
                System.err.println("Error initializing connection pool: " + e.getMessage());
            }
        }
        System.out.println("Database connection pool initialized. Current size: " + connectionPool.size());
    }

    /**
     * Returns the singleton instance of the {@code mysqlConnection}.
     *
     * @return the single instance of this class
     */
    public static synchronized mysqlConnection getInstance() {
        if (conn == null) {
            conn = new mysqlConnection();
        }
        return conn;
    }

    /**
     * Gets a new stand alone connection (not from the pool).
     * This is used when the background server thread requires a separate connection.
     * We don't want the server to get the pool connection and block a user (2 connection pools)
     * This is only for functions where no function was called by a user! (for example delete all late reservation)
     * Background function working (a thread that works every 30 second) it gets this connection!
     * @return a new {@link Connection} to the database, or {@code null} if failed
     */
    public Connection serverGetConnection() {
        try {
            return DriverManager.getConnection(DB_url, DB_USER, DB_Password);
        } catch (SQLException e) {
            return null;
        }
    }

    /**
     * Retrieves a connection from the pool, or creates a new one if the pool is not full.
     * If the connection is invalid, it will be replaced.
     *
     * @return a valid {@link Connection} to the database
     * @throws SQLException if unable to get or create a connection
     * @throws InterruptedException if interrupted while waiting for a connection
     */
    public Connection getConnection() throws SQLException, InterruptedException {
        Connection connection = connectionPool.poll(5, TimeUnit.SECONDS);
        if (connection == null) {
            synchronized (this) {
                if (connectionPool.size() < Max_Pool_size) {
                    try {
                        System.out.println("Pool exhausted, attempting to create a new connection...");
                        connection = DriverManager.getConnection(DB_url, DB_USER, DB_Password);
                        System.out.println("New connection created and provided.");
                    } catch (SQLException e) {
                        System.err.println("Failed to create new connection: " + e.getMessage());
                        throw new SQLException("Failed to get a connection from the pool or create a new one.", e);
                    }
                } else {
                    throw new SQLException("Database connection pool exhausted. Max connections reached.");
                }
            }
        } else {
            System.out.println(" > Connection retrieved from pool. Remaining: " + connectionPool.size());
        }

        if (connection != null && !connection.isValid(2)) {
            System.out.println(" > Invalid connection detected, attempting to replace it.");
            connectionPool.remove(connection);
            try {
                return DriverManager.getConnection(DB_url, DB_USER, DB_Password);
            } catch (SQLException e) {
                System.err.println("Failed to replace invalid connection: " + e.getMessage());
                throw new SQLException("Failed to replace invalid connection.", e);
            }
        }

        return connection;
    }
    /**
     * Inserts a new worker into the 'worker' table if the WorkerID doesn't already exist.
     *
     * @param conn     A valid JDBC Connection to the database.
     * @param workerId The worker's ID (e.g., "0000").
     * @param name     The worker's name (e.g., "Usher").
     * @param type     The worker's type (0 = worker, 1 = manager, etc.).
     * @throws SQLException if a database access error occurs.
     */
    public static void insertIfNotExists(Connection conn, String workerId,int type, String name) throws SQLException {
        String sql =
            "INSERT INTO worker (WorkerID, Type, Name) " +
            "SELECT * FROM (SELECT ? AS WorkerID, ? AS Type, ? AS Name) AS tmp " +
            "WHERE NOT EXISTS (SELECT 1 FROM worker WHERE WorkerID = ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, workerId);
            pstmt.setInt(2, type);
            pstmt.setString(3, name);
            pstmt.setString(4, workerId); // for WHERE NOT EXISTS
            pstmt.executeUpdate();
        }
    }
    
    /**
     * Creates Schema if not exits!
     * Creates all required tables in the `bpark` database if they do not already exist.
     * This method is useful during initial setup or development to ensure all schema structures are available.
     * 
     * Tables created:
     * <ul>
     *     <li>subscriber</li>
     *     <li>parkinghistory</li>
     *     <li>parkingspot</li>
     *     <li>reservation</li>
     *     <li>parkingreport</li>
     *     <li>subscriberreport</li>
     *     <li>tagreader</li>
     *     <li>worker</li>
     * </ul>
     *
     * @param con the database connection to use for executing table creation queries
     */
    public static void createAllTables() {
    	
    	        try {
    	            // Step 1: Create database if not exists
    	            try (Connection tempCon = DriverManager.getConnection(DB_URL_WITHOUT_SCHEMA, DB_USER, DB_Password);
    	                 Statement stmt = tempCon.createStatement()) {
    	                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS bpark");
    	               
    	            }

    	            // Step 2: Connect to the newly created (or existing) database
    	            try (Connection con = DriverManager.getConnection(DB_url, DB_USER, DB_Password);
    	                 Statement stmt = con.createStatement()) {


    	                // Step 3: Create all required tables

    	                stmt.executeUpdate("""
    	                    CREATE TABLE IF NOT EXISTS subscriber (
    	                        SubscriberID VARCHAR(45) NOT NULL,
    	                        UserName VARCHAR(45),
    	                        PhoneNumber VARCHAR(45),
    	                        Email VARCHAR(45),
    	                        PRIMARY KEY (SubscriberID)
    	                    )
    	                """);

    	                stmt.executeUpdate("""
    	                    CREATE TABLE IF NOT EXISTS parkingspot (
    	                        SpotID INT NOT NULL,
    	                        InUse TINYINT,
    	                        PRIMARY KEY (SpotID)
    	                    )
    	                """);

    	                stmt.executeUpdate("""
    	                    CREATE TABLE IF NOT EXISTS reservation (
    	                        ReservationID INT NOT NULL,
    	                        SubscriberID VARCHAR(45),
    	                        SpotID INT,
    	                        StartTime DATETIME,
    	                        EndTime DATETIME,
    	                        PRIMARY KEY (ReservationID),
    	                        FOREIGN KEY (SpotID) REFERENCES parkingspot(SpotID),
    	                        FOREIGN KEY (SubscriberID) REFERENCES subscriber(SubscriberID)
    	                    )
    	                """);

    	                stmt.executeUpdate("""
    	                    CREATE TABLE IF NOT EXISTS parkinghistory (
    	                        HistoryID INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    	                        subscriber VARCHAR(45),
    	                        SpotID INT,
    	                        resID INT,
    	                        EntryTime DATETIME,
    	                        ExitTime DATETIME,
    	                        Late TINYINT,
    	                        totalTimeParking BIGINT,
    	                        Extensions TINYINT,
    	                        ShowedUp TINYINT,
    	                        LateEmailSent TINYINT DEFAULT 0,
    	                        TimeToPark INT,
    	                        FOREIGN KEY (SpotID) REFERENCES parkingspot(SpotID),
    	                        FOREIGN KEY (subscriber) REFERENCES subscriber(SubscriberID)
    	                    )
    	                """);

    	                stmt.executeUpdate("""
    	                    CREATE TABLE IF NOT EXISTS parkingreport (
    	                        DateOfReport DATETIME,
    	                        Report_data BLOB,
    	                        PRIMARY KEY (DateOfReport)
    	                    )
    	                """);

    	                stmt.executeUpdate("""
    	                	    CREATE TABLE IF NOT EXISTS subscriberreport (
    	                	        SubscriberID VARCHAR(45) NOT NULL,
    	                	        DateOfReport VARCHAR(45) NOT NULL,
    	                	        Report_img BLOB,
    	                	        PRIMARY KEY (DateOfReport, SubscriberID)
    	                	    )
    	                	""");


    	                stmt.executeUpdate("""
    	                    CREATE TABLE IF NOT EXISTS tagreader (
    	                        TagReaderID INT NOT NULL,
    	                        SubscriberID VARCHAR(45),
    	                        PRIMARY KEY (TagReaderID),
    	                        FOREIGN KEY (SubscriberID) REFERENCES subscriber(SubscriberID)
    	                    )
    	                """);

    	                stmt.executeUpdate("""
    	                    CREATE TABLE IF NOT EXISTS worker (
    	                        WorkerID VARCHAR(45) NOT NULL,
    	                        Type TINYINT,
    	                        Name VARCHAR(45),
    	                        PRIMARY KEY (WorkerID)
    	                    )
    	                """);
    	               
    	                	

    	            }

    	        } catch (SQLException e) {
    	            System.err.println(" Error during database initialization: " + e.getMessage());
    	            e.printStackTrace();
    	        }
    	    }
    /**
     * Creates 100 parking spots where in use = 0! 
     * ID starts from 0 to 99! if any parking spot with the same id exists, we don't create such a parking spot!
     * This method is useful during initial setup or development to make it easier.
     * 
     * 
     * @param con the database connection to use for executing table creation queries
     */
    public static void createMissingParkingSpots(Connection conn) {
        int[] spotExist = new int[100];  // array[0] to array[99] default is 0
        
        String selectQuery = "SELECT SpotID FROM parkingspot";
        String insertQuery = "INSERT INTO parkingspot (SpotID, InUse) VALUES (?, 0)";

        try (
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(selectQuery);
        ) {
            // Step 1: Mark existing SpotIDs
            while (rs.next()) {
                int spotId = rs.getInt("SpotID");
                if (spotId >= 0 && spotId < 100) {
                    spotExist[spotId] = 1;
                }
            }

            // Step 2: Insert missing spots
            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                for (int i = 0; i < 100; i++) {
                    if (spotExist[i] == 0) {
                        insertStmt.setInt(1, i);
                        insertStmt.executeUpdate();
                    }
                }
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    /**
     * Removes expired reservations that started more than 15 minutes ago and were never used (no parkinghistory record).
     * Inserts a missed reservation entry into the history table for each deleted reservation.
     *
     * @param con an active {@link Connection} to the database
     * @return a list of reservation IDs that were deleted
     */
    public List<String> cleanExpiredReservationsAndReturnIds(Connection con) {
        List<String> deletedReservations = new ArrayList<>();

        String selectQuery = """
            SELECT ReservationID, SubscriberID, SpotID, StartTime, EndTime
            FROM reservation
            WHERE NOW() > StartTime + INTERVAL 15 MINUTE
        """;

        try (PreparedStatement selectStmt = con.prepareStatement(selectQuery);
             ResultSet rs = selectStmt.executeQuery()) {

            while (rs.next()) {
                String reservationId = rs.getString("ReservationID");
                String subscriberId = rs.getString("SubscriberID");
                String spotID = rs.getString("SpotID");
                Timestamp startTime = rs.getTimestamp("StartTime");
                Timestamp endTime = rs.getTimestamp("EndTime");

                long minutesToPark = Duration.between(startTime.toLocalDateTime(), endTime.toLocalDateTime()).toMinutes();
                if (minutesToPark < 0) minutesToPark = 0;

                String checkQuery = "SELECT COUNT(*) FROM parkinghistory WHERE resID = ?";
                try (PreparedStatement checkStmt = con.prepareStatement(checkQuery)) {
                    checkStmt.setString(1, reservationId);
                    ResultSet checkRs = checkStmt.executeQuery();
                    checkRs.next();

                    int arrived = checkRs.getInt(1);

                    if (arrived == 0) {
                        String deleteQuery = "DELETE FROM reservation WHERE ReservationID = ?";
                        try (PreparedStatement deleteStmt = con.prepareStatement(deleteQuery)) {
                            deleteStmt.setString(1, reservationId);
                            int deleted = deleteStmt.executeUpdate();

                            if (deleted > 0) {
                                deletedReservations.add(reservationId);
                                insertMissedReservationIntoHistory(con, subscriberId, spotID, reservationId, (int) minutesToPark, startTime);
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Error in cleanExpiredReservationsAndReturnIds: " + e.getMessage());
            e.printStackTrace();
        }

        return deletedReservations;
    }

    /**
     * Inserts a missed reservation into the `parkinghistory` table.
     * This occurs when a subscriber fails to show up within the allowed window.
     * We inserted into totalTimeParking 15 minutes, so we can know this user held this parking spot for 15 minutes
     * ShowedUp--> 0 so technically he wasted 15 minutes we want to see this in history for records!
     * @param con             The active database connection
     * @param subscriberId    The ID of the subscriber who missed the reservation
     * @param spotId          The ID of the reserved parking spot
     * @param reservationId   The ID of the reservation (currently unused in insertion)
     * @param minutesToPark   The intended parking duration in minutes
     * @param startTime       The start time of the reservation
     */
    public void insertMissedReservationIntoHistory(Connection con, String subscriberId, String spotId, String reservationId, int minutesToPark, Timestamp startTime) {
        String insertQuery = """
            INSERT INTO parkinghistory 
            (subscriber, SpotID, EntryTime, ExitTime, Late, Extensions, totalTimeParking, ShowedUp, LateEmailSent, TimeToPark)
            VALUES (?, ?, ?, ?, 0, 0, 15, 0, 0, ?)
        """;
        Timestamp exitTime = new Timestamp(startTime.getTime() + 15 * 60 * 1000);

        try (PreparedStatement stmt = con.prepareStatement(insertQuery)) {
            stmt.setString(1, subscriberId);
            stmt.setString(2, spotId);
            stmt.setTimestamp(3, startTime);
            stmt.setTimestamp(4, exitTime);
            stmt.setInt(5, minutesToPark);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error inserting missed reservation into parkinghistory: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Executes a SQL SELECT query in the background on a separate thread and returns the result via a callback.
     * Useful for keeping the UI responsive.
     * This is a Function Used By server only! no pool connection is used!
     * After we re-factored our code we saw that many SQL functions repeat their selves
     * We Wrote this function that can be called for every SQL request!
     * @param query     The SQL query to execute (must be a SELECT)
     * @param params    The parameters to bind in the PreparedStatement
     * @param callback  A function to handle the result string once the query completes
     */
    public void executeQueryInBackground(String query, Object[] params, Consumer<String> callback) {
        new Thread(() -> {
            StringBuilder result = new StringBuilder();
            Connection conn = null;

            try {
                conn = getConnection();
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    for (int i = 0; i < params.length; i++) {
                        stmt.setObject(i + 1, params[i]);
                    }

                    try (ResultSet rs = stmt.executeQuery()) {
                        ResultSetMetaData metaData = rs.getMetaData();
                        int columnCount = metaData.getColumnCount();

                        while (rs.next()) {
                            for (int i = 1; i <= columnCount; i++) {
                                result.append(rs.getString(i));
                                if (i < columnCount) result.append(" ");
                            }
                            result.append("\n");
                        }

                        // Remove trailing newline
                        if (result.length() > 0 && result.charAt(result.length() - 1) == '\n') {
                            result.deleteCharAt(result.length() - 1);
                        }
                    }
                }
            } catch (Exception e) {
                EchoServer.messageController.appendMessage("SQL error (background): " + e.getMessage());
                callback.accept(null);
                return;
            } finally {
                releaseConnection(conn);
            }

            callback.accept(result.toString());
        }).start();
    }

    /**
     * Releases a database connection back to the connection pool if it's still valid.
     * If the connection is invalid or cannot be returned to the pool, it is closed.
     *
     * @param connection The {@link Connection} to release
     */
    public void releaseConnection(Connection connection) {
        if (connection != null) {
            try {
                if (connection.isValid(1) && connectionPool.offer(connection)) {
                    System.out.println(" < Connection released back to pool. Current size: " + connectionPool.size());
                } else {
                    System.err.println(" < Connection invalid or could not be added to pool. Closing.");
                    connection.close();
                }
            } catch (SQLException e) {
                System.err.println("Error validating or closing connection: " + e.getMessage());
            }
        }
    }

    /**
     * Closes all active connections currently stored in the pool.
     * This should be called during application shutdown to release resources.
     */
    public void closeAllConnections() {
        System.out.println("Closing all connections in the pool...");
        while (!connectionPool.isEmpty()) {
            Connection connection = connectionPool.poll();
            if (connection != null) {
                try {
                    connection.close();
                    System.out.println(" > Connection closed.");
                } catch (SQLException e) {
                    System.err.println("Error closing pooled connection: " + e.getMessage());
                }
            }
        }
        System.out.println("All pooled connections closed.");
    }

    /**
     * Gets the current number of available connections in the connection pool.
     *
     * @return the current pool size
     */
    public int getCurrentPoolSize() {
        return connectionPool.size();
    }

    /**
     * Gets the maximum number of connections that the pool can hold.
     *
     * @return the maximum pool size
     */
    public int getMaxPoolSize() {
        return Max_Pool_size;
    }

    /**
     * Retrieves the ID of a single available parking spot that is not in use and
     * is not reserved for the next 15 minutes.
     *
     * @return a string containing the available SpotID, or {@code null} if none are available
     * @throws InterruptedException if thread is interrupted while waiting for a connection
     */
    public String getAvilableSpots() throws InterruptedException {
        String mainQuery = """
            SELECT ps.SpotID
            FROM parkingspot ps
            WHERE ps.InUse = 0
              AND NOT EXISTS (
                SELECT 1
                FROM reservation r
                WHERE r.SpotID = ps.SpotID
                  AND NOW() BETWEEN r.StartTime AND DATE_ADD(r.StartTime, INTERVAL 15 MINUTE)
              )
            LIMIT 1;
        """;

        String result = executeQuery(mainQuery);
        if (result == null || result.isEmpty()) {
            EchoServer.messageController.appendMessage("Error while checking available spots.\n");
            return null;
        }

        return result;
    }

    /**
     * Counts the number of currently empty parking spots that are not in use and
     * are not reserved for the next 15 minutes.
     *
     * @return a string representing the count of available spots, or {@code null} if an error occurred
     * @throws InterruptedException if thread is interrupted while waiting for a connection
     */
    public String COUNTEmptySpot() throws InterruptedException {
        String mainQuery = """
            SELECT COUNT(*)
            FROM parkingspot ps
            WHERE ps.InUse = 0
              AND NOT EXISTS (
                SELECT 1
                FROM reservation r
                WHERE r.SpotID = ps.SpotID
                  AND NOW() BETWEEN r.StartTime AND DATE_ADD(r.StartTime, INTERVAL 15 MINUTE)
              )
        """;

        String result = executeQuery(mainQuery);
        if (result == null || result.isEmpty()) {
            EchoServer.messageController.appendMessage("Error while checking available spots.\n");
            return null;
        }

        return result;
    }
  
    /**
     * Attempts to reserve a parking spot for a subscriber within the requested time range.
     * It performs checks for duplicate reservations on the same day, evaluates parking availability,
     * Check that there is 40% empty parks (on the same day of reserving!)
     * This prevents users from reserving 100% of parking spots!
     * selects an available spot, generates a new reservation ID, and inserts the reservation into the database.
     *
     * @param subscriberId        the ID of the subscriber
     * @param requestedStartTime  the desired reservation start time
     * @param requestedEndTime    the desired reservation end time
     * @return a string indicating result:
     *         - "1 <ReservationID>" if reservation is successful
     *         - "DUPLICATE_DATE" if a reservation already exists for that day
     *         - "0" if no available spots
     *         - "-1" if an error occurs
     * @throws InterruptedException if interrupted while accessing the database
     */
    public String reserve(String subscriberId, Timestamp requestedStartTime, Timestamp requestedEndTime) throws InterruptedException {
        Connection conn = null;

        try {
            // Step 0: Check if there's already a reservation on the same day
            String checkDupQuery = """
                SELECT COUNT(*) FROM reservation
                WHERE SubscriberID = ? AND DATE(StartTime) = ?
            """;
            String dupResult = executeQuery(checkDupQuery, subscriberId, new java.sql.Date(requestedStartTime.getTime()));
            if (dupResult != null && !dupResult.isEmpty()) {
                try {
                    if (Integer.parseInt(dupResult.trim()) > 0) return "DUPLICATE_DATE";
                } catch (NumberFormatException e) {
                    EchoServer.messageController.appendMessage("Error parsing duplicate check result.\n");
                    return "-1";
                }
            }

            // Step 1: Check parking spot availability
            String totalSpotsStr = executeQuery("SELECT COUNT(*) FROM parkingspot");
            String emptySpotsStr = getNumberOfAvailableSpotsDuring(requestedStartTime, requestedEndTime);
            if (totalSpotsStr == null || emptySpotsStr == null) return "-1";

            int totalSpots = Integer.parseInt(totalSpotsStr.trim());
            int emptySpots = Integer.parseInt(emptySpotsStr.trim());

            // If less than 40% availability, reject the reservation
            if (totalSpots == 0 || ((double) emptySpots / totalSpots) < 0.4) return "-1";

            // Step 2: Find an available spot
            String availableSpotQuery = """
                SELECT ps.SpotID
                FROM parkingspot ps
                WHERE ps.SpotID NOT IN (
                      SELECT r.SpotID FROM reservation r
                      WHERE NOT (r.EndTime <= ? OR r.StartTime >= ?)
                  )
                LIMIT 1
            """;
            String spotIDStr = executeQuery(availableSpotQuery, requestedStartTime, requestedEndTime);
            if (spotIDStr == null || spotIDStr.isEmpty()) return "0";

            int spotID = Integer.parseInt(spotIDStr.trim());

            // Step 3: Get a new Reservation ID (handle empty table case)
            String maxIdStr = executeQuery("SELECT COALESCE(MAX(ReservationID), 0) FROM reservation");
            int nextReservationId;
            try {
                nextReservationId = Integer.parseInt(maxIdStr.trim()) + 1;
            } catch (NumberFormatException e) {
                EchoServer.messageController.appendMessage("Error parsing max ReservationID.\n");
                return "-1";
            }

            // Step 4: Insert the new reservation
            conn = getConnection();
            String insertQuery = """
                INSERT INTO reservation (ReservationID, SubscriberID, SpotID, StartTime, EndTime)
                VALUES (?, ?, ?, ?, ?)
            """;

            try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
                stmt.setInt(1, nextReservationId);
                stmt.setString(2, subscriberId);
                stmt.setInt(3, spotID);
                stmt.setTimestamp(4, requestedStartTime);
                stmt.setTimestamp(5, requestedEndTime);

                int affectedRows = stmt.executeUpdate();
                return affectedRows > 0 ? "1 " + nextReservationId : "-1";
            }

        } catch (SQLException e) {
            EchoServer.messageController.appendMessage("SQL error in reserve(): " + e.getMessage());
            return "-1";
        } finally {
            releaseConnection(conn);
        }
    }

    /**
     * Retrieves the number of parking spots that are available between two timestamps.
     * A spot is considered available if it is not reserved during the given period.
     *
     * @param requestedStartTime the start time of the desired interval
     * @param requestedEndTime   the end time of the desired interval
     * @return a string representing the number of available spots
     * @throws InterruptedException if interrupted while accessing the database
     */
    public String getNumberOfAvailableSpotsDuring(Timestamp requestedStartTime, Timestamp requestedEndTime) throws InterruptedException {
        String query = """
            SELECT COUNT(*)
            FROM parkingspot ps
            WHERE ps.SpotID NOT IN (
                SELECT r.SpotID
                FROM reservation r
                WHERE NOT (r.EndTime <= ? OR r.StartTime >= ?)
            )
        """;

        return executeQuery(query, requestedStartTime, requestedEndTime);
    }

    /**
     * Updates the phone number and email address for a specific subscriber.
     *
     * @param subscriberID the subscriber's ID
     * @param phone        the new phone number
     * @param email        the new email address
     * @return true if the update was successful, false otherwise
     * @throws InterruptedException if interrupted while accessing the database
     */
    public boolean updateSubscriberPersonalData(String subscriberID, String phone, String email) throws InterruptedException {
        String query = "UPDATE subscriber SET PhoneNumber = ?, Email = ? WHERE SubscriberID = ?";
        return executeUpdate(query, phone, email, subscriberID);
    }

    /**
     * Retrieves personal information for a subscriber, including ID, name, phone, and email.
     *
     * @param subscriberID the subscriber's ID
     * @return a space-separated string in the format "SubscriberID UserName PhoneNumber Email",
     *         or an error message if not found or incomplete
     * @throws InterruptedException if interrupted while accessing the database
     */
    public String getSubscriberPersonalData(String subscriberID) throws InterruptedException {
        String query = "SELECT SubscriberID, UserName, PhoneNumber, Email FROM subscriber WHERE SubscriberID = ?";
        ArrayList<String> info = executeQueryAsList(query, subscriberID);

        if (info == null || info.isEmpty() || info.size() < 4) {
            return "No subscriber data found.";
        }

        return String.join(" ", info); // returns: "123456 John 050-1234567 john@email.com"
    }
    
    /**
     * Retrieves the parking history of a subscriber where the car has exited.
     *
     * @param subscriberID the ID of the subscriber
     * @return a formatted string listing the parking sessions, or a message if none found
     * @throws InterruptedException if the database query is interrupted
     */
    public String getHistoryBySubscriberID(String subscriberID) throws InterruptedException {
    	 String checkQuery = "SELECT 1 FROM subscriber WHERE SubscriberID = ?";
         String result0=executeQuery(checkQuery,subscriberID);
             if(result0==null||result0=="")
             {
                     EchoServer.messageController.appendMessage("Subscriber doesn't exist: " + subscriberID);
                     return "No Such Subscriber!";
                 }
             
        String query = """
            SELECT EntryTime, ExitTime, Late, totalTimeParking, Extensions
            FROM parkinghistory
            WHERE subscriber = ? AND ExitTime IS NOT NULL
            ORDER BY EntryTime DESC
        """;

        String rawResult = executeQuery(query, subscriberID);

        if (rawResult == null || rawResult.isEmpty()) {
            return "No history found for subscriber: " + subscriberID;
        }

        StringBuilder result = new StringBuilder();
        String[] rows = rawResult.split("\n");

        for (String row : rows) {
            String[] columns = row.split(" ");
            if (columns.length >= 5) {
                result.append(String.format(
                    "Start: %s | End: %s | Late: %s | Duration: %s min | Extensions: %s\n",
                    columns[0], columns[1], columns[2], columns[3], columns[4]
                ));
            }
        }

        return result.toString();
    }

    /**
     * Retrieves the current parking status of a subscriber (if the car has not exited yet).
     *
     * @param subID the subscriber ID
     * @return a string containing the spot ID, entry time, and allowed time to park; or null if not found
     */
    public String GetSubscriberCurrentParkingStatus(String subID) {
        String checkHistoryQuery = "SELECT SpotID,EntryTime,TimeToPark FROM parkinghistory WHERE subscriber=? AND ExitTime IS NULL";
        try {
            return executeQuery(checkHistoryQuery, subID);
        } catch (InterruptedException e) {
            EchoServer.messageController.appendMessage("Error while getting parking status for user: " + subID + "\n");
            EchoServer.messageController.appendMessage("Error: " + e.getMessage() + "\n");
            return null;
        }
    }

    /**
     * Finalizes a parking session for a subscriber by updating the history and spot status.
     *
     * @param subID            the subscriber ID
     * @param spotID           the parking spot ID
     * @param isLate           "1" if the subscriber is late, "0" otherwise
     * @param totalTimeParking the total parking duration in minutes
     * @param parkingID        the history record ID
     * @return true if the update was successful, false otherwise
     */
    public boolean retriveCarForSubscriber(String subID, String spotID, String isLate, String totalTimeParking, String parkingID) {
        String updateSpotQuery = "UPDATE bpark.parkingspot SET InUse = 0 WHERE SpotID = ? AND InUse = 1";
        String retriveCarQuery = "UPDATE bpark.parkinghistory SET ExitTime = NOW(), Late = ?, totalTimeParking = ? WHERE SpotID = ? AND subscriber = ? AND HistoryID = ? AND ExitTime IS NULL";

        try {
            boolean updated = executeUpdateQuery(retriveCarQuery, isLate, totalTimeParking, spotID, subID, parkingID);
            if (!updated) {
                EchoServer.messageController.appendMessage("Failed to update parking history.\n");
                return false;
            }

            updated = executeUpdateQuery(updateSpotQuery, spotID);
            if (!updated) {
                EchoServer.messageController.appendMessage("Failed to update parking spot status.\n");
                return false;
            }

            return true;
        } catch (InterruptedException e) {
            EchoServer.messageController.appendMessage("Error while finalizing parking for user: " + subID + "\n");
            EchoServer.messageController.appendMessage("Error: " + e.getMessage() + "\n");
            return false;
        }
    }

    /**
     * Attempts to assign a parking spot to a subscriber and create a new parking session.
     * It ensures the subscriber is not already parked and that the selected spot is not reserved soon.
     *
     * @param minimumMinutesBeforeNextReservation the minimum time required before the next reservation
     * @param subID                               the subscriber ID
     * @return the new HistoryID as a string if successful, or a message/error/null if not
     * @throws InterruptedException if the query execution is interrupted
     */
    public String getParkingCode(int minimumMinutesBeforeNextReservation, String subID) throws InterruptedException {
        // Step 1: Check if subscriber already has an active parking session
        String checkHistoryQuery = "SELECT HistoryID FROM parkinghistory WHERE subscriber=? AND ExitTime IS NULL";
        String PastHistoryID = executeQuery(checkHistoryQuery, subID);

        if (PastHistoryID != null && !PastHistoryID.isEmpty()) {
            EchoServer.messageController.appendMessage("You already parked your car.\n");
            return "You/already/parked/your/car.";
        }

        // Step 2: Find available spot not reserved in the next X minutes
        String findSpotQuery = """
            SELECT ps.SpotID
            FROM parkingspot ps
            WHERE ps.InUse = 0
              AND NOT EXISTS (
                  SELECT 1
                  FROM reservation r
                  WHERE r.SpotID = ps.SpotID
                    AND NOW() BETWEEN r.StartTime AND DATE_ADD(r.StartTime, INTERVAL 15 MINUTE)
              )
              AND NOT EXISTS (
                  SELECT 1
                  FROM reservation r
                  WHERE r.SpotID = ps.SpotID
                    AND r.StartTime > NOW()
                    AND TIMESTAMPDIFF(MINUTE, NOW(), r.StartTime) < ?
              )
            LIMIT 1;
        """;

        String spotID = executeQuery(findSpotQuery, minimumMinutesBeforeNextReservation);
        if (spotID == null || spotID.isEmpty()) {
            EchoServer.messageController.appendMessage("Error while checking available spots.\n");
            return null;
        }

        // Step 3: Mark the spot as in use
        String updateSpotQuery = "UPDATE bpark.parkingspot SET InUse = 1 WHERE SpotID = ?";
        boolean updated = executeUpdateQuery(updateSpotQuery, spotID);

        if (!updated) {
            EchoServer.messageController.appendMessage("Failed to update spot as occupied.\n");
            return null;
        }

        // Step 4: Insert the parking session into the history table
        String insertHistoryQuery = """
            INSERT INTO bpark.parkinghistory (subscriber, SpotID, resID, EntryTime, TimeToPark, Extensions, ShowedUp)
            VALUES (?, ?, NULL, NOW(), ?, 0, 1)
        """;

        int historyID = executeInsertReturningID(insertHistoryQuery, subID, spotID, minimumMinutesBeforeNextReservation);

        if (historyID == -1) {
            EchoServer.messageController.appendMessage("Failed to insert parking history.\n");
            String updateSpotQueryFailed = "UPDATE bpark.parkingspot SET InUse = 0 WHERE SpotID = ?";
            boolean updatedFailed = executeUpdateQuery(updateSpotQueryFailed, spotID);
            if(updatedFailed==true)
            { EchoServer.messageController.appendMessage("retrieved spot!");}
            return null;
        }

        return String.valueOf(historyID);
    }
    
    /**
     * Executes an update SQL query (INSERT, UPDATE, DELETE) with parameters.
     *
     * @param query  the SQL query string
     * @param params the query parameters
     * @return true if rows were affected, false otherwise
     * @throws InterruptedException if the thread is interrupted while waiting for a connection
     */
    public boolean executeUpdateQuery(String query, Object... params) throws InterruptedException {
        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
                int affectedRows = stmt.executeUpdate();
                return affectedRows > 0;
            }
        } catch (SQLException e) {
            EchoServer.messageController.appendMessage("SQL error in executeUpdateQuery: " + e.getMessage());
            return false;
        } finally {
            releaseConnection(conn);
        }
    }

    /**
     * Executes an INSERT SQL query and returns the generated key (e.g., auto-increment ID).
     *
     * @param query  the SQL INSERT query
     * @param params the parameters to bind to the query
     * @return the generated ID if successful, or -1 if failed
     * @throws InterruptedException if the thread is interrupted while waiting for a connection
     */
    public int executeInsertReturningID(String query, Object... params) throws InterruptedException {
        Connection conn = null;
        int generatedID = -1;

        try {
            conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }

                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Insert failed, no rows affected.");
                }

                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        generatedID = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Insert succeeded but no ID returned.");
                    }
                }
            }
        } catch (SQLException e) {
            EchoServer.messageController.appendMessage("SQL error in executeInsertReturningID: " + e.getMessage());
        } finally {
            releaseConnection(conn);
        }

        return generatedID;
    }

    /**
     * Retrieves a formatted list of currently active parking spots (not exited).
     * Uses JOIN between parkinghistory and subscriber tables to fetch user and parking info.
     *
     * @author Amit_Regev
     * @return a formatted string with user names, parking code, entry time, and duration
     * @throws InterruptedException if interrupted while accessing the database
     */
    public String getActiveParkingSpots() throws InterruptedException {
        StringBuilder result = new StringBuilder();
        String query = """
            SELECT ph.HistoryID, s.UserName, ph.EntryTime, ph.TimeToPark
            FROM parkinghistory ph, subscriber s
            WHERE ph.ExitTime IS NULL AND ph.subscriber = s.subscriberID
        """;

        Connection conn = null;

        try {
            conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    int historyId = rs.getInt("HistoryID");
                    String userName = rs.getString("UserName");
                    Timestamp EntryTime = rs.getTimestamp("EntryTime");
                    int timetoPark = rs.getInt("TimeToPark");

                    int tmphours = timetoPark / 60;
                    int tmpminutes = timetoPark % 60;
                    String timeFormatted = String.format("%d hours and %d minutes", tmphours, tmpminutes);

                    String line = String.format(
                        "Name: %s | ParkingCode: %d | EntryTime: %s | TimeToPark: %s",
                        userName, historyId, EntryTime.toString(), timeFormatted);
                    result.append(line).append("\n");
                }

            }
        } catch (SQLException e) {
            EchoServer.messageController.appendMessage("SQL error in getActiveParkingSpots: " + e.getMessage());
            return null;
        } finally {
            releaseConnection(conn);
        }

        if (result.length() > 0) {
            result.setLength(result.length() - 1); // Remove the last newline
        }

        return result.toString();
    }

    /**
     * Checks if a subscriber exists and if the given name matches the name associated with the ID.
     * This is used for validating terminal sign-ins.
     *
     * @author Amit_Regev
     * @param subscriberId the subscriber ID to verify
     * @param subscriberName the name to match against the database
     * @return subscriber info string if verified, null otherwise
     * @throws InterruptedException if interrupted while accessing the database
     */
    public String getSubscriberID(String subscriberId, String subscriberName) throws InterruptedException {
        String mainQuery = "SELECT * FROM subscriber WHERE SubscriberID = ?";
        String info = executeQuery(mainQuery, subscriberId);

        if (info == null || info.isEmpty()) {
            EchoServer.messageController.appendMessage("Subscriber ID not found: " + subscriberId);
            return null;
        }

        String[] parts = info.split(" ");
        if (parts.length < 2 || !parts[1].equalsIgnoreCase(subscriberName)) {
            EchoServer.messageController.appendMessage("Subscriber name does not match for ID: " + subscriberId);
            return null;
        }

        return info;
    }

    /**
     * Retrieves a worker's information from the database based on their ID and type.
     *
     * @param workerId The ID of the worker.
     * @param Type The type of worker to match.
     * @return The worker info as a space-separated string, or null if not found or mismatched.
     * @throws InterruptedException if the query execution is interrupted.
     */
    public String getWorkerID(String workerId, String Type) throws InterruptedException {
        String mainQuery = "SELECT * FROM worker WHERE WorkerID = ? AND Type = ?";
        String info = executeQuery(mainQuery, workerId, Type);

        if (info == null || info.isEmpty()) {
            EchoServer.messageController.appendMessage(
                "Either Worker ID not found or this type doesn't suit the worker: " + workerId
            );
            return null;
        }
        return info;
    }

    /**
     * Fetches raw monthly parking statistics from the parkinghistory table.
     * Includes total parking time (in minutes), number of late entries, late exits,
     * number of extensions, reservation usage, cancellations, and most frequent entry hour.
     *
     * @param from Start date (inclusive) for filtering parking entries.
     * @param to   End date (exclusive) for filtering parking entries.
     * @param con  Active SQL connection to use
     * @return Map with keys:
     *   "TotalMinutes", "Late", "LateExits", "Extensions", "MostRequestedHour",
     *   "ReservationCount", "CancelledReservations", "LateReservationArrivals"
     * @author Amit_Regev
     */
    public Map<String, String> getMonthlyParkingStatsRaw(LocalDate from, LocalDate to, Connection con) {
        Map<String, String> result = new HashMap<>();

        String query = """
            SELECT 
                SUM(TIMESTAMPDIFF(MINUTE, EntryTime, ExitTime)) AS TotalMinutes,
                SUM(CASE WHEN Late = 1 THEN 1 ELSE 0 END) AS LateCount,
                SUM(CASE WHEN LateEmailSent = 1 THEN 1 ELSE 0 END) AS LateExitCount,
                SUM(CASE WHEN Extensions = 1 THEN 1 ELSE 0 END) AS ExtensionCount,
                SUM(CASE WHEN resID IS NOT NULL THEN 1 ELSE 0 END) AS ReservationCount,
                SUM(CASE WHEN resID IS NOT NULL AND ShowedUp = 0 THEN 1 ELSE 0 END) AS CancelledReservations,
                SUM(CASE WHEN resID IS NOT NULL AND Late = 1 THEN 1 ELSE 0 END) AS LateReservationArrivals,
                (
                    SELECT HOUR(EntryTime)
                    FROM parkinghistory
                    WHERE EntryTime >= ? AND EntryTime < ?
                    GROUP BY HOUR(EntryTime)
                    ORDER BY COUNT(*) DESC
                    LIMIT 1
                ) AS MostRequestedHour
            FROM parkinghistory
            WHERE EntryTime >= ? AND EntryTime < ? AND ExitTime IS NOT NULL
        """;

        try (PreparedStatement stmt = con.prepareStatement(query)) {
            Timestamp fromTimestamp = Timestamp.valueOf(from.atStartOfDay());
            Timestamp toTimestamp = Timestamp.valueOf(to.atStartOfDay());

            stmt.setTimestamp(1, fromTimestamp);
            stmt.setTimestamp(2, toTimestamp);
            stmt.setTimestamp(3, fromTimestamp);
            stmt.setTimestamp(4, toTimestamp);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    result.put("TotalMinutes", rs.getString("TotalMinutes"));
                    result.put("Late", rs.getString("LateCount"));
                    result.put("LateExits", rs.getString("LateExitCount"));
                    result.put("Extensions", rs.getString("ExtensionCount"));
                    result.put("MostRequestedHour", rs.getString("MostRequestedHour"));
                    result.put("ReservationCount", rs.getString("ReservationCount"));
                    result.put("CancelledReservations", rs.getString("CancelledReservations"));
                    result.put("LateReservationArrivals", rs.getString("LateReservationArrivals"));
                }
            }
        } catch (Exception e) {
            EchoServer.messageController.appendMessage("SQL error in getMonthlyParkingStatsRaw: " + e.getMessage());
        }

        return result;
    }

    
    /**
     * Retrieves a subscriber's monthly report image from the database.
     * Returns the report image as a byte array (PNG format) for the specified subscriber and date.
     *
     * @param subscriberId the subscriber's ID
     * @param reportDate the report date
     * @return byte[] containing the image, or null if not found
     */
    public byte[] getSubscriptionReport(String subscriberId, Date reportDate) throws InterruptedException {
        String query = "SELECT Report_img FROM subscriberreport WHERE SubscriberID = ? AND DateOfReport = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, subscriberId);
            stmt.setDate(2, reportDate);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBytes("Report_img");
            } else {
                return null;
            }

        } catch (SQLException e) {
            EchoServer.messageController.appendMessage("SQL error in getSubscriptionReport: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Executes a SELECT query and returns the first row as a list of string values.
     *
     * @param query The SQL query to execute.
     * @param params Optional parameters to bind to the query.
     * @return A list of strings representing the first row of results, or null if an error occurs.
     * @throws InterruptedException if query execution is interrupted.
     */
    public ArrayList<String> executeQueryAsList(String query, Object... params) throws InterruptedException {
        ArrayList<String> result = new ArrayList<>();
        Connection conn = null;

        try {
            conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    if (rs.next()) {
                        for (int i = 1; i <= columnCount; i++) {
                            result.add(rs.getString(i));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            EchoServer.messageController.appendMessage("SQL error in executeQueryAsList: " + e.getMessage());
            return null;
        } finally {
            releaseConnection(conn);
        }

        return result;
    }
    
    /**
     * Retrieves the subscriber ID associated with a specific tag reader.
     *
     * @param tagReaderId The ID of the tag reader.
     * @return The subscriber ID if found, or null otherwise.
     * @throws InterruptedException if query execution is interrupted.
     */
    public String getSubscriberIdByTagReader(String tagReaderId) throws InterruptedException {
        String query = "SELECT SubscriberID FROM tagreader WHERE TagReaderID = ?";
        String subscriberID = executeQuery(query, tagReaderId);

        if (subscriberID == null || subscriberID.isEmpty()) {
            EchoServer.messageController.appendMessage("TagReader ID not found: " + tagReaderId);
            return null;
        }
        return subscriberID;
    }
    
    /**
     * Finds all subscribers who have parked and exceeded their allowed time,
     * updates the database to mark them as notified, and returns the result via callback.
     *
     * @param con The database connection to use.
     * @param callback A consumer that accepts the result list (each map contains subscriber details).
     */
    public void getOverdueParkingsInBackground(Connection con, Consumer<List<Map<String, String>>> callback) {
        String query = """
            SELECT ph.subscriber, s.Email, s.UserName, ph.EntryTime, ph.ShowedUp, ph.TimeToPark
            FROM parkinghistory ph
            JOIN subscriber s ON ph.subscriber = s.SubscriberID
            WHERE ph.ExitTime IS NULL AND ph.LateEmailSent = FALSE
        """;

        new Thread(() -> {
            List<Map<String, String>> results = new ArrayList<>();

            try (
                PreparedStatement selectStmt = con.prepareStatement(query);
                ResultSet rs = selectStmt.executeQuery();
                PreparedStatement updateStmt = con.prepareStatement("""
                    UPDATE parkinghistory
                    SET LateEmailSent = TRUE
                    WHERE subscriber = ? AND ExitTime IS NULL
                """)
            ) {
                while (rs.next()) {
                    String showedUp = rs.getString("ShowedUp");
                    if (!"1".equals(showedUp)) {
                        continue; // Only notify if the user actually showed up
                    }

                    String subscriberId = rs.getString("subscriber");
                    String email = rs.getString("Email");
                    String name = rs.getString("UserName");
                    Timestamp rawTs = rs.getTimestamp("EntryTime");
                    int timeToPark = rs.getInt("TimeToPark");

                    // Check if time to park has expired
                    LocalDateTime entryTime = rawTs.toLocalDateTime();
                    LocalDateTime deadline = entryTime.plusMinutes(timeToPark);
                    LocalDateTime now = LocalDateTime.now();

                    if (now.isBefore(deadline)) {
                        continue; // Not yet overdue
                    }

                    // User is overdue - add to list
                    String formattedEntryTime = entryTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    Map<String, String> record = new HashMap<>();
                    record.put("SubscriberID", subscriberId);
                    record.put("Email", email);
                    record.put("UserName", name);
                    record.put("EntryTime", formattedEntryTime);
                    record.put("TimeToPark", Integer.toString(timeToPark));
                    results.add(record);

                    // Update LateEmailSent to TRUE
                    updateStmt.setString(1, subscriberId);
                    updateStmt.executeUpdate();

                    EchoServer.messageController.appendMessage("Late parking detected for: " + subscriberId);
                }

            } catch (Exception e) {
                EchoServer.messageController.appendMessage("LateParking query error: " + e.getMessage());
                e.printStackTrace();
            }

            callback.accept(results);
        }).start();
    }

    /**
     * Finds all subscribers who have parked and exceeded their allowed time after 4 hours!,
     * Force Car To Exit
     *
     * @param con The database connection to use.
     * @param callback A consumer that accepts the result list (each map contains subscriber details).
     */
    public void ExitLateParkingsAfter4HOURS(Connection con, Consumer<List<Map<String, String>>> callback) {
        String query = """
            SELECT ph.subscriber, s.Email, s.UserName, ph.EntryTime, ph.ShowedUp, ph.TimeToPark, ph.SpotID
            FROM parkinghistory ph
            JOIN subscriber s ON ph.subscriber = s.SubscriberID
            WHERE ph.ExitTime IS NULL AND ph.LateEmailSent = TRUE
        """;

        new Thread(() -> {
            List<Map<String, String>> results = new ArrayList<>();

            try (
                PreparedStatement selectStmt = con.prepareStatement(query);
                ResultSet rs = selectStmt.executeQuery();
                PreparedStatement updateStmt = con.prepareStatement("""
                    UPDATE parkinghistory
                    SET ExitTime = ?, totalTimeparking = ?, Late = 1
                    WHERE subscriber = ? AND ExitTime IS NULL
                """);
        		PreparedStatement updateStmt2 = con.prepareStatement("""
                        UPDATE parkingspot
                        SET InUse = 0
                        WHERE SpotID = ? 
                    """);
            ) {
                while (rs.next()) {
                    String showedUp = rs.getString("ShowedUp");
                    if (!"1".equals(showedUp)) {
                        continue; // Only notify if the user actually showed up
                    }

                    String subscriberId = rs.getString("subscriber");
                    String email = rs.getString("Email");
                    String userName = rs.getString("UserName");
                    Timestamp rawTs = rs.getTimestamp("EntryTime");
                    int timeToPark = rs.getInt("TimeToPark");
                    String spotID = rs.getString("SpotID");

                    LocalDateTime entryTime = rawTs.toLocalDateTime();
                    LocalDateTime deadline = entryTime.plusMinutes(timeToPark + 240); // 4 hours
                    LocalDateTime now = LocalDateTime.now();

                    if (now.minusMinutes(1).isBefore(deadline)) {
                        continue; // Not yet overdue
                    }

                    String formattedExitTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                    Map<String, String> record = new HashMap<>();
                    record.put("SubscriberID", subscriberId);
                    record.put("Email", email);
                    record.put("UserName", userName);
                    record.put("EntryTime", entryTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    record.put("TimeToPark", Integer.toString(timeToPark));
                    record.put("ExitTime", formattedExitTime);
                    results.add(record);

                    // Update ExitTime and totalTimeparking
                    updateStmt.setTimestamp(1, Timestamp.valueOf(now));
                    updateStmt.setInt(2, timeToPark + 240); // total time = original + 4 hours
                    updateStmt.setString(3, subscriberId);
                    updateStmt.executeUpdate();
                    
                    //Update parking spot InUse status
                    updateStmt2.setString(1, spotID);
                    updateStmt2.executeUpdate(); 

                    EchoServer.messageController.appendMessage(
                        "Moving user (" + subscriberId + ") car out of parking spot"
                    );
                }

            } catch (Exception e) {
                EchoServer.messageController.appendMessage("LateParking query error: " + e.getMessage());
                e.printStackTrace();
            } finally {
                callback.accept(results);
            }

        }).start();
    }




    /**
     * Retrieves the active parking code for a subscriber.
     * The result is formatted as: "1 <HistoryID> <Email> <UserName...>".
     * Returns "0" if no active parking is found for the subscriber.
     *
     * @param subscriberId The subscriber's ID.
     * @return A formatted string containing the history ID, email, and user name, or "0" if not found.
     * @throws InterruptedException If the thread is interrupted while querying.
     */
    public String SendCode(String subscriberId) throws InterruptedException {
        String query = """
            SELECT ph.HistoryID, s.Email, s.UserName
            FROM parkinghistory ph
            JOIN subscriber s ON ph.Subscriber = s.SubscriberID
            WHERE ph.Subscriber = ? AND ph.ExitTime IS NULL
            LIMIT 1
        """;

        String result = executeQuery(query, subscriberId);

        if (result != null && !result.isBlank()) {
            return "1 " + result.trim();
        }

        return "0"; // no active code found
    }

    /**
     * Executes a SQL SELECT query and returns the result as a single string.
     * The result is formatted with values separated by spaces and rows separated by newlines.
     *
     * @param query  The SQL query string.
     * @param params Optional parameters to bind to the query.
     * @return The formatted result string, or null if an error occurs.
     * @throws InterruptedException If the thread is interrupted during query execution.
     */
    public String executeQuery(String query, Object... params) throws InterruptedException {
        StringBuilder result = new StringBuilder();
        Connection conn = null;

        try {
            conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    while (rs.next()) {
                        for (int i = 1; i <= columnCount; i++) {
                            result.append(rs.getString(i));
                            if (i < columnCount) result.append(" ");
                        }
                        result.append("\n");
                    }

                    if (result.length() > 0 && result.charAt(result.length() - 1) == '\n') {
                        result.deleteCharAt(result.length() - 1);
                    }
                }
            }
        } catch (SQLException e) {
            EchoServer.messageController.appendMessage("SQL error in executeQuery: " + e.getMessage());
            return null;
        } finally {
            releaseConnection(conn);
        }

        return result.toString();
    }
    
    /**
     * Allows a subscriber to park using a valid reservation.
     * This method checks reservation validity, updates spot usage, and inserts a new parking history entry.
     *
     * @param subscriberID The subscriber's ID.
     * @param reservationID The reservation ID.
     * @return Status message indicating success or failure.
     * @throws InterruptedException If the thread is interrupted during the operation.
     */
    public String parkWithReservation(String subscriberID, String reservationID) throws InterruptedException {
        // Step 1: Get StartTime and SpotID from reservation
        String reservationQuery = """
            SELECT StartTime, SpotID, EndTime
            FROM reservation
            WHERE ReservationID = ? AND SubscriberID = ?
        """;

        ArrayList<String> reservationData = executeQueryAsList(reservationQuery, reservationID, subscriberID);

        if (reservationData == null || reservationData.size() < 2) {
            EchoServer.messageController.appendMessage("Reservation not found for subscriber: " + subscriberID);
            return "PARKING_RESERVATION_FAILED";
        }

        String startTimeStr = reservationData.get(0);
        String spotID = reservationData.get(1);
        String EndTimestr = reservationData.get(2);

        // Step 2: Parse times and check if user is late
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime;
        try {
            startTime = LocalDateTime.parse(startTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            EchoServer.messageController.appendMessage("Error parsing reservation StartTime: " + startTimeStr);
            return "PARKING_RESERVATION_FAILED_TIME_PARSE";
        }

        LocalDateTime EndTime;
        try {
            EndTime = LocalDateTime.parse(EndTimestr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            EchoServer.messageController.appendMessage("Error parsing reservation EndTimestr: " + EndTimestr);
            return "PARKING_RESERVATION_FAILED_TIME_PARSE";
        }

        if (startTime.isAfter(now)) {
            return "PARKING_RESERVATION_FAILED_EARLY_ARRIVE";
        }

        boolean isLate = now.isAfter(startTime.plusMinutes(1));
        int late = isLate ? 1 : 0;

        String checkUsedQuery = "SELECT COUNT(*) FROM parkinghistory WHERE resID = ?";
        String existingHistory = executeQuery(checkUsedQuery, reservationID);

        if (existingHistory != null && Integer.parseInt(existingHistory) > 0) {
            EchoServer.messageController.appendMessage("Reservation already used by subscriber: " + subscriberID);
            return "PARKING_RESERVATION_ALREADY_USED";
        }

        // Step 3: Update spot as occupied
        String updateSpotQuery = "UPDATE parkingspot SET InUse = 1 WHERE SpotID = ?";
        if (!executeUpdate(updateSpotQuery, spotID)) {
            EchoServer.messageController.appendMessage("Failed to update spot " + spotID + " as occupied.");
            return "PARKING_RESERVATION_FAILED_UPDATE_SPOTID";
        }

        // Step 4: Insert parking history
        String insertHistoryQuery = """
            INSERT INTO parkinghistory 
            (subscriber, SpotID, resID, EntryTime, Late, Extensions, ShowedUp, TimeToPark, LateEmailSent)
            VALUES (?, ?, ?, NOW(), ?, 0, 1, ?, 0)
        """;

        long minutesToPark = Duration.between(now, EndTime).toMinutes();
        int historyID = executeInsertReturningID(insertHistoryQuery, subscriberID, spotID, reservationID, late, (int) minutesToPark);

        if (historyID == -1) {
            EchoServer.messageController.appendMessage("Failed to insert parking history.");
            return "PARKING_RESERVATION_FAILED_INSERT_HISTORY";
        }

        EchoServer.messageController.appendMessage("Reservation parking success for subscriber: " + subscriberID + ", HistoryID: " + historyID);
        return "PARKING_RESERVATION_SUCCESS " + historyID;
    }

    /**
     * Retrieves all subscriber records from the database and prints them.
     *
     * @return A list of all subscribers.
     * @throws InterruptedException If the thread is interrupted during the query.
     */
    public ArrayList<String> getAllSubscribers() throws InterruptedException {
        ArrayList<String> subscribers = new ArrayList<>();
        String mainQuery = "SELECT * FROM subscriber";
        String rs = executeQuery(mainQuery);

        if(!rs.isEmpty()) {
        	String[] str = rs.split(" ");
            for (String s : str) {
                System.out.println(s);
                subscribers.add(s);
            }

            return subscribers;
        }
        return null;
    }

    /**
     * Executes an SQL update query (such as INSERT, UPDATE, or DELETE).
     *
     * @param query  The SQL query to execute.
     * @param params Parameters to bind to the prepared statement.
     * @return {@code true} if at least one row was affected, {@code false} otherwise.
     * @throws InterruptedException If the thread is interrupted during query execution.
     */
    public boolean executeUpdate(String query, Object... params) throws InterruptedException {
        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }

                int affectedRows = stmt.executeUpdate();
                return affectedRows > 0;
            }
        } catch (SQLException e) {
            EchoServer.messageController.appendMessage("SQL error in executeUpdate: " + e.getMessage());
            return false;
        } finally {
            releaseConnection(conn);
        }
    }
    
    /**
     * Adds a new subscriber to the database.
     * If the subscriber already exists (by name, phone, and email), an error is returned.
     * The SubscriberID is automatically generated in the format "SUB###".
     *
     * @param message A list containing [UserName, PhoneNumber, Email].
     * @return The new subscriber ID (e.g., "SUB124") on success, or an error code:
     *         "ERROR_EXISTS" if subscriber already exists,
     *         "ERROR_INSERT" if insertion fails.
     * @throws InterruptedException If the thread is interrupted during the operation.
     */
    public String addNewSubscriber(ArrayList<String> message) throws InterruptedException {
        Connection conn = null;
        try {
            conn = getConnection();

            // Step 1: Check if subscriber already exists (same name, phone, and email)
            String checkQuery = "SELECT COUNT(*) FROM subscriber WHERE UserName = ? AND PhoneNumber = ? AND Email = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, message.get(0)); // UserName
            checkStmt.setString(2, message.get(1)); // PhoneNumber
            checkStmt.setString(3, message.get(2)); // Email
            ResultSet rs = checkStmt.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                EchoServer.messageController.appendMessage("Subscriber already exists: " + message.get(0));
                return "ERROR_EXISTS";
            }

            // Step 2: Generate a new subscriber ID ("SUB###")
            String idQuery = "SELECT MAX(CAST(SUBSTRING(SubscriberID, 4) AS UNSIGNED)) FROM subscriber WHERE SubscriberID LIKE 'SUB%'";
            PreparedStatement idStmt = conn.prepareStatement(idQuery);
            ResultSet idRs = idStmt.executeQuery();
            int nextId = 1;
            if (idRs.next()) {
                nextId = idRs.getInt(1) + 1;
            }
            String newSubscriberId = "SUB" + nextId;

            // Step 3: Insert the new subscriber into the database
            String insertQuery = "INSERT INTO subscriber (SubscriberID, UserName, PhoneNumber, Email) VALUES (?, ?, ?, ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
            insertStmt.setString(1, newSubscriberId);
            insertStmt.setString(2, message.get(0));
            insertStmt.setString(3, message.get(1));
            insertStmt.setString(4, message.get(2));

            int rowsInserted = insertStmt.executeUpdate();
            if (rowsInserted > 0) {
                return newSubscriberId; // return generated ID (e.g., "SUB124")
            } else {
                return "ERROR_INSERT"; // if no row was inserted
            }

        } catch (SQLException e) {
            EchoServer.messageController.appendMessage("SQL error in addNewSubscriber: " + e.getMessage());
            return "ERROR_INSERT";

        } finally {
            if (conn != null) {
                try {
                    connectionPool.offer(conn); // Return connection to pool
                } catch (Exception ex) {
                    EchoServer.messageController.appendMessage("Failed to return connection to pool: " + ex.getMessage());
                }
            }
        }
    }
    /**
     * Adds a new tag reader to subscriber to the database.
     * If the subscriber doesn't exists (by ID), an error is returned.
     * The tag reader is automatically generated.
     *
     * @param message A list containing [SubscriberID].
     * @return The new TagReader ID on success, or an error code:
     *         "ERROR_NO_SUCH_SUBSCRIBER" if subscriber doesn't exist,
     *         "ERROR_SUBSCRIBER_ALREADY_HAS_TAG" if subscriber already have tag.
     *         "ERROR_INSERT_TAGREADER" if SQL fails to insert data (tag reader for subscriber)
     *         "ERROR_SQL" for other SQL Failure (catch) interruption 
     * @throws InterruptedException If the thread is interrupted during the operation.
     */
    public String addTagToSubscriber(ArrayList<String> message) throws InterruptedException {
        Connection conn = null;
        try {
            conn = getConnection();

            String subscriberId = message.get(0);  // User ID

            // Step 1: Check if subscriber exists
            String checkQuery = "SELECT 1 FROM subscriber WHERE SubscriberID = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                checkStmt.setString(1, subscriberId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (!rs.next()) {
                        EchoServer.messageController.appendMessage("Subscriber doesn't exist: " + subscriberId);
                        return "ERROR_NO_SUCH_SUBSCRIBER";
                    }
                }
            }

            // Step 1.5: Check if subscriber already has a TagReader
            String checkTagQuery = "SELECT 1 FROM tagreader WHERE SubscriberID = ?";
            try (PreparedStatement tagCheckStmt = conn.prepareStatement(checkTagQuery)) {
                tagCheckStmt.setString(1, subscriberId);
                try (ResultSet tagCheckRs = tagCheckStmt.executeQuery()) {
                    if (tagCheckRs.next()) {
                        EchoServer.messageController.appendMessage("Subscriber " + subscriberId + " already has a TagReader.");
                        return "ERROR_SUBSCRIBER_ALREADY_HAS_TAG";
                    }
                }
            }

            // Step 2: Generate a random unique TagReaderID
            int newTagId;
            boolean isUnique;
            do {
                newTagId = (int) (Math.random() * 1_000_000);  // Random number between 0 and 999999
                String tagIdCheckQuery = "SELECT 1 FROM tagreader WHERE TagReaderID = ?";
                try (PreparedStatement tagIdCheckStmt = conn.prepareStatement(tagIdCheckQuery)) {
                    tagIdCheckStmt.setInt(1, newTagId);
                    try (ResultSet tagIdRs = tagIdCheckStmt.executeQuery()) {
                        isUnique = !tagIdRs.next();  // True if ID is unique
                    }
                }
            } while (!isUnique);

            // Step 3: Insert the new TagReader
            String insertQuery = "INSERT INTO tagreader (TagReaderID, SubscriberID) VALUES (?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                insertStmt.setInt(1, newTagId);
                insertStmt.setString(2, subscriberId);

                int rowsInserted = insertStmt.executeUpdate();
                if (rowsInserted > 0) {
                    EchoServer.messageController.appendMessage("TagReader " + newTagId + " added to subscriber " + subscriberId);
                    return "ADD_TAG_SUCCESS " + subscriberId + " " + newTagId;
                } else {
                    return "ERROR_INSERT_TAGREADER";
                }
            }

        } catch (SQLException e) {
            EchoServer.messageController.appendMessage("SQL error in addTagToSubscriber: " + e.getMessage());
            return "ERROR_SQL";
        } finally {
            if (conn != null) {
                try {
                    connectionPool.offer(conn); // Return connection to pool
                } catch (Exception ex) {
                    EchoServer.messageController.appendMessage("Failed to return connection to pool: " + ex.getMessage());
                }
            }
        }
    }


    
    /**
     * Retrieves all parking history records (with entry and exit time) for all subscribers
     * during the given date range. Only completed records (with non-null ExitTime) are returned.
     *
     * @param from start date (inclusive)
     * @param to end date (exclusive)
     * @return a list of maps, each map includes keys: "SubscriberID", "EntryTime", "ExitTime", "Late"
     */
    public List<Map<String, String>> getFullHistoryForMonth(LocalDate from, LocalDate to,Connection con) throws InterruptedException {
        List<Map<String, String>> results = new ArrayList<>();

        String query = """
        	    SELECT subscriber, EntryTime, ExitTime, Late, LateEmailSent, Extensions, resID, ShowedUp
        	    FROM parkinghistory
        	    WHERE EntryTime >= ? AND EntryTime < ? AND ExitTime IS NOT NULL
        	""";


        try (PreparedStatement stmt = con.prepareStatement(query)) {

            stmt.setTimestamp(1, Timestamp.valueOf(from.atStartOfDay()));
            stmt.setTimestamp(2, Timestamp.valueOf(to.atStartOfDay()));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> row = new HashMap<>();
                    row.put("SubscriberID", rs.getString("subscriber"));
                    row.put("EntryTime", rs.getTimestamp("EntryTime").toLocalDateTime().toString());
                    row.put("ExitTime", rs.getTimestamp("ExitTime").toLocalDateTime().toString());
                    row.put("Late", rs.getString("Late"));
                    row.put("LateEmailSent", rs.getString("LateEmailSent"));
                    row.put("Extensions", rs.getString("Extensions"));
                    row.put("resID", rs.getString("resID"));
                    row.put("ShowedUp", rs.getString("ShowedUp"));

                    results.add(row);
                }
            }
        } catch (SQLException e) {
            EchoServer.messageController.appendMessage("SQL error in getFullHistoryForMonth: " + e.getMessage());
        }

        return results;
    }
    
    /**
     * Saves multiple subscriber report images in a single batch insert to the database.
     *@author Amit_Regev
     * @param reportDate the logical report date (e.g., 2025-07-01 for June report)
     * @param reportDataList list of report entries: each is a map with subscriberId and imageBytes
     * @return true if all inserts succeeded
     */
    public boolean saveSubscriberReportsBatch(LocalDate reportDate, List<Map<String, Object>> reportDataList, Connection con) throws InterruptedException {
        String query = """
            INSERT INTO subscriberreport (SubscriberID, DateOfReport, Report_img)
            VALUES (?, ?, ?)
        """;

        try (PreparedStatement stmt = con.prepareStatement(query)) {

            Date sqlReportDate = java.sql.Date.valueOf(reportDate); 
            for (Map<String, Object> data : reportDataList) {
                stmt.setString(1, (String) data.get("SubscriberID"));         // SubscriberID
                stmt.setDate(2, sqlReportDate);                               // DateOfReport
                stmt.setBytes(3, (byte[]) data.get("ImageBytes"));            // Report_img
                stmt.addBatch();
            }

            int[] results = stmt.executeBatch();
            return results.length == reportDataList.size();

        } catch (SQLException e) {
            EchoServer.messageController.appendMessage("SQL error in saveSubscriberReportsBatch: " + e.getMessage());
            return false;
        }
    }

    
    /**
     * Saves the visual monthly report chart into the database as a BLOB.
     * Uses the first day of the following month as the report date.
     *
     * @param reportDate  the first day of the month (e.g., 2025-06-01 for May report)
     * @param imageBytes  the image data (PNG format)
     * @return true if saved successfully
     */
    public boolean saveParkingReportImage(LocalDate reportDate, byte[] imageBytes,Connection con) throws InterruptedException {
        String query = """
            INSERT INTO parkingreport (DateOfReport, Report_data)
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE Report_data = VALUES(Report_data)
        """;

        

        try {
            try (PreparedStatement stmt = con.prepareStatement(query)) {
                stmt.setDate(1, java.sql.Date.valueOf(reportDate));
                stmt.setBytes(2, imageBytes);
                int rows = stmt.executeUpdate();
                return rows > 0;
            }
        } catch (SQLException e) {
            EchoServer.messageController.appendMessage("SQL error while saving parking report image: " + e.getMessage());
            return false;
        } 
    }
    
    /**
     * @author Amit_Regev
     * Retrieves the saved monthly parking report image (as a byte array) from the database.
     * This report is stored as a BLOB in the 'parkingreport' table.
     * @param reportDate The date used as the primary key (e.g., 2025-06-01).
     * @return The report image as byte[], or null if not found.
     * @throws InterruptedException if database connection is interrupted.
     */
    public byte[] getParkingReportByDate(Date reportDate) throws InterruptedException {
        String query = "SELECT Report_data FROM parkingreport WHERE DateOfReport = ?";
        Connection conn = null;

        try {
            conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setDate(1, reportDate);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getBytes("Report_data");
                    }
                }
            }
        } catch (SQLException e) {
            EchoServer.messageController.appendMessage("SQL error in getParkingReportImage: " + e.getMessage());
        } finally {
            releaseConnection(conn);
        }

        return null; // no image found
    }
    
    /**
     * Checks if a record exists in the database for the given query and parameters.
     *
     * @param query  The SQL SELECT query to execute.
     * @param params Parameters to bind to the prepared statement.
     * @return {@code true} if at least one record exists, {@code false} otherwise.
     * @throws InterruptedException If the thread is interrupted during the operation.
     */
    public boolean recordExists(String query, Object... params) throws InterruptedException {
        Connection conn = null;

        try {
            conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next(); // returns true if at least one result exists
                }
            }
        } catch (SQLException e) {
            EchoServer.messageController.appendMessage("SQL error in recordExists: " + e.getMessage());
            return false;
        } finally {
            releaseConnection(conn);
        }
    }
    
    
    
    /**
     * @author Amit_Regev
     * Checks whether the given subscriber currently has an active parking entry.
     * Active means there's a record in parking history where ExitTime IS NULL.
     *
     * @param subscriberID The subscriber's ID
     * @return true if the subscriber is currently parked, false otherwise
     */
    public boolean hasActiveParking(String subscriberID) throws InterruptedException {
        String query = "SELECT 1 FROM parkinghistory WHERE subscriber = ? AND ExitTime IS NULL LIMIT 1";
        Connection conn = null;

        try {
            conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, subscriberID);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();  
                }
            }
        } catch (SQLException e) {
            EchoServer.messageController.appendMessage("SQL error in hasActiveParking: " + e.getMessage());
            return false;
        } finally {
            releaseConnection(conn);
        }
    }

    /**
     * Attempts to extend the active parking session for a subscriber
     * for a user-defined number of minutes (up to 240).
     *
     * Rules:
     * - Extension allowed only once.
     * - Requested duration must not conflict with future reservations.
     * - A reservation must have at least 1 hour buffer before its start.
     *
     * @param subscriberID The subscriber ID requesting the extension.
     * @param totalMinutes The number of minutes requested for extension.
     * @return A result message indicating success or reason for denial.
     * @throws InterruptedException if database access is interrupted.
     */
    public String extendParkingDurationInDB(String subscriberID, int totalMinutes) throws InterruptedException, SQLException {

        if (totalMinutes <= 0 || totalMinutes > 240) {
            return "EXTENSION_DENIED: Requested duration must be between 1 and 240 minutes.";
        }

        Connection conn = null;

        try {
            conn = getConnection();

            // Step 1: Retrieve active parking info
            String query = """
                SELECT HistoryID, SpotID, Extensions
                FROM parkinghistory
                WHERE subscriber = ? AND ExitTime IS NULL
            """;

            int historyID = -1;
            int spotID = -1;
            int extensions = 0;

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, subscriberID);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        historyID = rs.getInt("HistoryID");
                        spotID = rs.getInt("SpotID");
                        extensions = rs.getInt("Extensions");
                    } else {
                        return "ERROR: No active parking found (unexpected).";
                    }
                }
            }

            // Step 2: Check if already extended
            if (extensions == 1) {
                return "EXTENSION_DENIED: You have already used your one-time extension.";
            }

         // Step 3: Check for any reservation in the next 4 hours - if exists, deny extension
            String futureResQuery = """
                SELECT StartTime
                FROM reservation
                WHERE SpotID = ?
                  AND StartTime > NOW()
                  AND StartTime <= DATE_ADD(NOW(), INTERVAL 4 HOUR)
                ORDER BY StartTime ASC
                LIMIT 1
            """;

            try (PreparedStatement stmt = conn.prepareStatement(futureResQuery)) {
                stmt.setInt(1, spotID);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return "EXTENSION_DENIED: Cannot extend because a future reservation is scheduled within 4 hours.";
                    }
                }
            }


         // Step 4: Mark extension as used and update TimeToPark
            String updateQuery = """
                UPDATE parkinghistory
                SET Extensions = 1, TimeToPark = TimeToPark + ?
                WHERE HistoryID = ?
            """;

            try (PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
                stmt.setInt(1, totalMinutes);  
                stmt.setInt(2, historyID);
                int updated = stmt.executeUpdate();
                if (updated == 0) {
                    return "ERROR: Failed to record extension and update time.";
                }
            }

            // return message after successful update
            return "EXTENSION_GRANTED: Your parking is now extended by " + totalMinutes + " minutes.";

            } finally {
                releaseConnection(conn);
            }

    }

}


