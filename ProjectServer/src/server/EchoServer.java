package server;
import java.net.InetAddress;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.mysql.cj.xdevapi.Client;

import common.MyFile;
import gui.ServerMessageFrameController;
import javafx.application.Platform;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;
import common.MyFile;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * <p>
 * EchoServer is the main server-side class responsible for handling client connections,
 * executing database queries, managing scheduled tasks such as checking parking states,
 * and generating automated monthly reports.
 * </p>
 * 
 * <p>
 * It extends AbstractServer from the OCSF framework and manages asynchronous query execution,
 * timed operations, client activity tracking, and handles database connection pooling through a singleton mysqlConnection.
 * </p>
 * 
 * @author Amit_Regev
 */
public class EchoServer extends AbstractServer {

    /** Tracks the last activity timestamp for each connected client */
    private final HashMap<ConnectionToClient, Long> lastActivity = new HashMap<>();

    /** Default server port if none is specified */
    public static final int DEFAULT_PORT = 5555;

    /** Singleton instance for database access */
    private mysqlConnection db;

    /** Dedicated background connection used for scheduled queries */
    private Connection serverBackgroundConnection;

    /** Thread pool for executing database queries asynchronously */
    private final ExecutorService queryExecutor;

    /** Reference to the JavaFX controller for UI logging and interaction */
    public static ServerMessageFrameController messageController;

    /** List of IPs that have been disconnected */
    private ArrayList<String> DisconnectedIPs = new ArrayList<>();

    /** Number of currently connected clients */
    private int numOfClients = 0;

    /** Holds security-related session data per client */
    private HashMap<ConnectionToClient, String[]> securityArray = new HashMap<>();

    /** Tracks the date on which the last monthly report was generated */
    private LocalDate lastMonthlyReportDate = null;

    /** Tracks the month for which the last monthly report was generated */
    private YearMonth lastMonthlyReportMonth = null;

    /**
     * Constructs a new EchoServer instance listening on the given port.
     * Initializes the DB connection, sets up a thread pool, and schedules background tasks.
     *
     * @param port the port number on which the server listens
     */
    public EchoServer(int port) {
        super(port);
   
        // Set server timezone
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Jerusalem"));

        // Initialize DB pool and print current size
        db = mysqlConnection.getInstance();
        EchoServer.messageController.appendMessage("Server Initialized! Current pool size: " + db.getCurrentPoolSize());
     
        // Prepare query thread pool
        queryExecutor = Executors.newFixedThreadPool(db.getMaxPoolSize() + 2);

        // Create a persistent background DB connection for tasks like report generation
        try {
            serverBackgroundConnection = db.serverGetConnection();
            EchoServer.messageController.appendMessage("Server background connection created once.");
    		}
         catch (Exception e) {
        	 EchoServer.messageController.appendMessage("Failed to create background DB connection: " + e.getMessage());
            e.printStackTrace();
        }
        //this is specially written for you Lecturers in order to make it easier for you to create the database
        //with two workers! and also 100 parking spots! if needed you can run this function! 
        try {
        	mysqlConnection.insertIfNotExists(serverBackgroundConnection, "0000",0,"Usher");
        	mysqlConnection.insertIfNotExists(serverBackgroundConnection, "1111", 1, "Manager");
        	  EchoServer.messageController.appendMessage("WorkerID(0000) Type(0) Name(Usher)");
        	  EchoServer.messageController.appendMessage("WorkerID(1111) Type(1) Name(Manager)");
        	  EchoServer.messageController.appendMessage("Created Successfully!");
        	  mysqlConnection.createMissingParkingSpots(serverBackgroundConnection);
        	  EchoServer.messageController.appendMessage("Created 100 parking spots IDs equals 0...99 & InUse equals 0!");
        }
        catch(Exception e)
        {
        	EchoServer.messageController.appendMessage("Error Creating database or inserting informations! "+e.getMessage());
        }
        	
        

        // Setup recurring background tasks
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Step 1: late parking & expired reservations
                if (messageController != null) {
                    checkForLateParkings(serverBackgroundConnection);
                    checkExpiredReservations(serverBackgroundConnection);
                    ExitParkingCarsAfter4HOURS(serverBackgroundConnection);
                } else {
                	 EchoServer.messageController.appendMessage("Waiting for messageController to be ready...");
                }

                // Step 2: monthly report check
                LocalDate today = LocalDate.now();
                LocalTime currentTime = LocalTime.now();
                //this line shows in message controller window the time when thread runs! 
                //messageController.appendMessage("Scheduler running: " + today + " " + currentTime);

                if (today.getDayOfMonth() == 1 && currentTime.getHour() == 0) {
                    messageController.appendMessage("Triggering monthly reports...");
                    if (!today.equals(lastMonthlyReportDate)) {
                        lastMonthlyReportDate = today;
                        generateMonthlyParkingReportAutomatically(serverBackgroundConnection);
                        generateAllSubscriberReportsAutomatically(serverBackgroundConnection);
                        
                    } else {
                        messageController.appendMessage("Monthly reports already generated today.");
                    }
                }

            } catch (InterruptedException e) {
            	 EchoServer.messageController.appendMessage("Scheduler thread was interrupted.");
                e.printStackTrace();
            } catch (Exception ex) {
            	 EchoServer.messageController.appendMessage("Error in scheduler: " + ex.getMessage());
                ex.printStackTrace();
            }
        }, 0, 30, TimeUnit.SECONDS); // Run scheduler every 30 seconds
    }

    /**
     * Checks the database for any expired reservations and removes them.
     * A reservation is considered expired if the subscriber has not arrived within 15 minutes.
     * Deleted reservation IDs are then displayed in the server UI.
     *
     * @param con the database connection to use for querying expired reservations
     */
    public void checkExpiredReservations(Connection con) {
        new Thread(() -> {
            List<String> deletedReservations = db.cleanExpiredReservationsAndReturnIds(con);

            if (!deletedReservations.isEmpty() && EchoServer.messageController != null) {
                Platform.runLater(() -> {
                    for (String id : deletedReservations) {
                        EchoServer.messageController.appendMessage(
                            "Reservation " + id + " was deleted (no arrival within 15 minutes)."
                        );
                    }
                });
            }
        }).start();
    }

    /**
     * Invoked when a new client connects to the server.
     * Adds the client's IP to the disconnected list (for tracking), initializes its security array,
     * and updates the connected clients count.
     *
     * @param client the newly connected client
     */
    @Override
    protected void clientConnected(ConnectionToClient client) {
        String ip = client.getInetAddress().getHostAddress();
        String[] u = {"", ""};
        securityArray.put(client, u);
        DisconnectedIPs.add(ip);
        numOfClients++;
        updateClientStatus("Client connected: " + ip);
    }

    /**
     * Disconnects all connected clients gracefully.
     * Sends a "SERVER_DISCONNECT" message to each client before closing the connection.
     */
    public void disconnectAllClients() {
        try {
            for (Thread clientThread : getClientConnections()) {
                if (clientThread instanceof ConnectionToClient) {
                    ConnectionToClient client = (ConnectionToClient) clientThread;
                    try {
                        client.close();
                    } catch (IOException e) {
                       
                    }
                } 
            }
        } catch (Exception e) {
            
        }
    }


    /**
     * Handles unexpected exceptions from a client.
     * Cleans up internal maps, updates the disconnected IP list, and refreshes the status view.
     *
     * @param client    the client that caused the exception
     * @param exception the exception thrown
     */
    @Override
    protected synchronized void clientException(ConnectionToClient client, Throwable exception) {
    	 EchoServer.messageController.appendMessage("Client exception: " + exception.getMessage());
        lastActivity.remove(client);
        securityArray.remove(client);

        ArrayList<String> Copy = new ArrayList<>(DisconnectedIPs);
        String[] existingIPs = getConnectedIPs().split(" ");

        for (int i = 0; i < existingIPs.length; i++) {
            if (Copy.contains(existingIPs[i])) {
                Copy.remove(existingIPs[i]);
            }
        }

        messageController.appendMessage("Disconnected IP:" + Copy.toString());

        if (!Copy.isEmpty()) {
            DisconnectedIPs.remove(Copy.get(0));
        }

        updateClientStatus(null);
    }


    /**
     * Called when a client disconnects from the server.
     * Removes the client from activity and security tracking and updates the status.
     *
     * @param client the client that disconnected
     */
    @Override
    protected synchronized void clientDisconnected(ConnectionToClient client) {
        lastActivity.remove(client);
        securityArray.remove(client);
        updateClientStatus(null);
    }

    /**
     * Called when the server starts listening for connections.
     * Updates the UI and starts the inactivity monitoring thread.
     */
    @Override
    protected void serverStarted() {
        updateClientStatus("Server is now listening on port " + getPort());
        startInactivityMonitor(); 
    }

    /**
     * Called when the server has stopped listening for connections.
     * Displays a message on the UI.
     */
    @Override
    protected void serverStopped() {
        Platform.runLater(() -> {
            messageController.appendMessage("Server has stopped listening for connections.");
        });
    }

    /**
     * Updates the status label and list of connected clients in the server UI.
     *
     * @param msg an optional message to display above the client count and IP list
     */
    private void updateClientStatus(String msg) {
        Platform.runLater(() -> {
            if (msg != null) {
                messageController.appendMessage(msg);
            }
            messageController.appendMessage("Connected clients: " + getNumberOfClients());
            printConnectedIPs();
        });
    }

    /**
     * Prints the IP addresses of all currently connected clients to the server UI.
     */
    private void printConnectedIPs() {
        StringBuilder sb = new StringBuilder("Connected IPs:\n");
        for (Thread t : getClientConnections()) {
            if (t instanceof ConnectionToClient) {
                ConnectionToClient client = (ConnectionToClient) t;
                if (client.getInetAddress() != null && client.getInetAddress().getHostAddress() != null) {
                    sb.append(client.getInetAddress().getHostAddress()).append("\n");
                }
                messageController.appendMessage(sb.toString());
            }
        }
    }

    /**
     * Retrieves the IP addresses of all currently connected clients.
     *
     * @return A space-separated string of IP addresses.
     */
    private String getConnectedIPs() {
        StringBuilder sb = new StringBuilder();
        for (Thread t : getClientConnections()) {
            if (t instanceof ConnectionToClient) {
                ConnectionToClient client = (ConnectionToClient) t;
                if (client.getInetAddress() != null && client.getInetAddress().getHostAddress() != null) {
                    sb.append(client.getInetAddress().getHostAddress()).append(" ");
                }
            }
        }
        return sb.toString();
    }

    /**
     * Handles messages received from a connected client. 
     * Parses the message, checks command type, and delegates to the appropriate handler.
     * Each command is expected to be in a List<String> format with a recognized command keyword at index 0.
     *
     * @param msg The message object received from the client (expected to be ArrayList<String>).
     * @param client The client that sent the message.
     */
    @Override
    public void handleMessageFromClient(Object msg, ConnectionToClient client) {
        lastActivity.put(client, System.currentTimeMillis());

        String clientIP = client.getInetAddress().getHostAddress();
        messageController.appendMessage("Client IP: " + clientIP);
        messageController.appendMessage("Message received: " + msg.toString() + " from " + client);

        queryExecutor.submit(() -> {
            ArrayList<String> messageList = safeCastToStringList(msg);
            if (messageList == null || messageList.isEmpty()) {
                sendToClientLabelUpdate(client, "Oops, Something Went Wrong\nInvalid message format!");
                return;
            }

            String command = messageList.get(0);

            try {
                switch (command) {
                    // user login commands
                   case "LOGOUT":
                	   messageController.appendMessage("Client requested logout: " + client);
                    	securityArray.remove(client); 
                 
                    break;
                   case "user sign in":
                        if (messageList.size() == 3)
                        {
                            handleUserSignIn(messageList.get(1), messageList.get(2), client, "Termenal");
                        }
                        else
                            sendToClientLabelUpdate(client, "Invalid data for user sign in.");
                        break;

                    case "user sign in away":
                        if (messageList.size() == 3)
                        {
                            handleUserSignIn(messageList.get(1), messageList.get(2), client, "Away");
                        }
                        else
                            sendToClientLabelUpdate(client, "Invalid data for user sign in.");
                        break;

                    case "worker sign in":
                        if (messageList.size() == 3)
                        {
                            handleWorkerSignIn(messageList.get(1), messageList.get(2), client);
                        }
                        else
                            sendToClientLabelUpdate(client, "Invalid data for worker sign in.");
                        break;

                    case "tagreader sign in":
                        if (messageList.size() == 2)
                        {
                            handleTagReaderSignIn(messageList.get(1), client);
                        }
                        else
                            sendToClientLabelUpdate(client, "Invalid data for tagreader sign in.");
                        break;

                    // report generation
                    case "SUBSCRIPTION_REPORT":
                        if ("Manager".equals(isClientOfType(client))) {
                            if (messageList.size() == 3)
                                handleSubscriptionReport(messageList.get(1), messageList.get(2), client);
                            else
                                sendToClientLabelUpdate(client, "Invalid data for subscription report.");
                        } else {
                            sendToClientLabelUpdate(client, "Invalid type of user for this report.");
                        }
                        break;

                    // other major functionalities (check, reserve, retrieve, extend, etc.)
                    case "Check_Avilable_Spots":
                        if (messageList.size() == 1)
                            handleCheckAvilableSpots(client, "Away");
                        else
                            sendToClientLabelUpdate(client, "Invalid data for checking available spots.");
                        break;

                    case "Check_Avilable_Spots_Termenal":
                        if (messageList.size() == 1)
                            handleCheckAvilableSpots(client, "Termenal");
                        else
                            sendToClientLabelUpdate(client, "Invalid data for available spots.");
                        break;

                    case "Get_ParkingCode_Termenal":
                        if (messageList.size() == 2)
                            handleGetNewParkingCode(client, messageList.get(1));
                        else
                            sendToClientLabelUpdate(client, "Invalid data for parking code. " + messageList.toString());
                        break;

                    case "PARKING_REPORT":
                        if (messageList.size() == 2)
                            handleParkingReport(messageList.get(1), client);
                        else
                            sendToClientLabelUpdate(client, "Invalid data for parking report.");
                        break;

                    case "GET_ACTIVE_PARKINGSPOT":
                        if ("Manager".equals(isClientOfType(client)) || "Usher".equals(isClientOfType(client))) {
                            if (messageList.size() == 1)
                                handleActivParkingspot(client);
                            else
                                sendToClientLabelUpdate(client, "Invalid data for active parking spot.");
                        }
                        break;

                    case "GET_ALL_SUBSCRIBERS":
                        if ("Manager".equals(isClientOfType(client)) || "Usher".equals(isClientOfType(client))) {
                            if (messageList.size() == 1)
                                handleGetAllSubscribers(client);
                            else
                                sendToClientLabelUpdate(client, "Invalid data for getting all subscribers.");
                        }
                        break;

                    case "ADD_TAG_READER":
                    	 messageList.remove(0);
                         try {
                             AddTagToSubscriber(client, messageList);
                         } catch (Exception ex) {
                             sendToClientLabelUpdate(client, ex.getMessage());
                         }
                         break;
                         
                    case "SHOW_SUBSCRIBER_HISTORY":
                   	 messageList.remove(0);
                        try {
                            ShowSubscriberHistory(client, messageList);
                        } catch (Exception ex) {
                            sendToClientLabelUpdate(client, ex.getMessage());
                        }
                        break;
                    
                    case "ADD_SUB":
                        messageList.remove(0);
                        try {
                            handleAddNewSubscriber(client, messageList);
                        } catch (Exception ex) {
                            sendToClientLabelUpdate(client, ex.getMessage());
                        }
                        break;

                    case "Reserve":
                        if (messageList.size() == 3) {
                            String dateTime = messageList.get(1);
                            String TimeToPark = messageList.get(2);
                            Reserve(client, dateTime, TimeToPark);
                        } else {
                            sendToClientLabelUpdate(client, "Invalid data for reservation.");
                        }
                        break;

                    case "Forgot_Code":
                        ForgotCode(client);
                        break;

                    case "GET_HISTORY":
                        try {
                            handleGetHistoryBySpecificSubscriber(client, messageList.get(1), messageList.get(2));
                        } catch (Exception ex) {
                            sendToClientLabelUpdate(client, ex.getMessage());
                        }
                        break;

                    case "Personal_Data":
                        try {
                            handleGetPersonalData(client, messageList.get(1), messageList.get(2));
                        } catch (Exception ex) {
                            sendToClientLabelUpdate(client, ex.getMessage());
                        }
                        break;

                    case "UPDATE_PERSONAL_DATA":
                        try {
                            handleUpdatePersonalData(client, messageList.get(1), messageList.get(2), messageList.get(3), messageList.get(4));
                        } catch (Exception ex) {
                            sendToClientLabelUpdate(client, ex.getMessage());
                        }
                        break;

                    case "Get_My_Parking_Status_Termenal":
                        if (messageList.size() == 1)
                            handleGetMyParkingStatus(client);
                        else
                            sendToClientLabelUpdate(client, "Invalid data for parking Status. " + messageList.toString());
                        break;

                    case "Check_Reserve":
                        if (messageList.size() == 2)
                            CheckReservation(client, messageList.get(1).trim());
                        else
                            sendToClientLabelUpdate(client, "Invalid format for reservation check. " + messageList.toString());
                        break;

                    case "Retrieve_Car_Termenal":
                        if (messageList.size() == 2)
                            handleRetrieveCar(client, messageList.get(1));
                        else
                            sendToClientLabelUpdate(client, "Invalid data for retrieve car. " + messageList.toString());
                        break;

                    case "CHECK_ACTIVE_PARKING":
                        if (messageList.size() == 3)
                            handleCheckActiveParking(client, messageList.get(1), messageList.get(2));
                        else
                            sendToClientLabelUpdate(client, "Invalid data for active parking check.");
                        break;

                    case "EXTEND_PARKING":
                        try {
                            messageController.appendMessage("Entered case EXTEND_PARKING.");
                            if (messageList.size() == 4) {
                                String subscriberID = messageList.get(1);
                                String totalMinutesStr = messageList.get(2);
                                String sourcePage = "SubscriberAwayController";
                                int totalMinutes = Integer.parseInt(totalMinutesStr);
                                messageController.appendMessage("Parsed values: subscriberID=" + subscriberID + ", totalMinutes=" + totalMinutes + ", sourcePage=" + sourcePage);
                                handleExtendParkingTime(client, subscriberID, totalMinutes, sourcePage);
                            } else {
                                sendToClientLabelUpdate(client, "Invalid data for parking time extension.");
                            }
                        } catch (Exception ex) {
                            sendToClientLabelUpdate(client, "Error during parking time extension: " + ex.getMessage());
                        }
                        break;

                    default:
                        sendToClientLabelUpdate(client, "Oops, something went wrong. Unrecognized message format.");
                        break;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                sendToClientLabelUpdate(client, "Internal server error: " + e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Checks whether the given reservation ID is valid for the subscriber and attempts to park the car accordingly.
     * Sends appropriate response messages back to the client based on the outcome.
     * 
     * @param client         the client connection that made the request
     * @param reservationID  the reservation code provided by the subscriber
     * @throws InterruptedException if the thread executing the task is interrupted
     * @throws IOException if sending data to the client fails
     */
    public void CheckReservation(ConnectionToClient client, String reservationID) throws InterruptedException, IOException {
        String subscriberId = this.securityArray.get(client)[1];
        String result = db.parkWithReservation(subscriberId, reservationID);

        // Split the result string: first part is status, second part is additional info (e.g., history ID)
        String[] parts = result.split(" ", 2);
        String status = parts[0];
        String extraInfo = (parts.length > 1) ? parts[1] : "";

        switch (status) {
            case "PARKING_RESERVATION_SUCCESS":
                // Send only success + historyID to client
                client.sendToClient("SubscriberTermenalController ParkWithReservation Success Parking code: " + extraInfo);
                EchoServer.messageController.appendMessage("Reservation success for subscriber: " + subscriberId + ", HistoryID: " + extraInfo);
                break;

            case "PARKING_RESERVATION_FAILED":
                client.sendToClient("SubscriberTermenalController ParkWithReservation Failed Reservation not found");
                break;

            case "PARKING_RESERVATION_FAILED_TIME_PARSE":
                client.sendToClient("SubscriberTermenalController ParkWithReservation Failed Invalid reservation time format");
                break;

            case "PARKING_RESERVATION_FAILED_UPDATE_SPOTID":
                client.sendToClient("SubscriberTermenalController ParkWithReservation Failed Could not mark spot as occupied");
                break;

            case "PARKING_RESERVATION_FAILED_INSERT_HISTORY":
                client.sendToClient("SubscriberTermenalController ParkWithReservation Failed Could not insert parking history");
                break;

            case "PARKING_RESERVATION_ALREADY_USED":
                client.sendToClient("SubscriberTermenalController ParkWithReservation Failed Reservation already used");
                EchoServer.messageController.appendMessage("Reservation already used for subscriber: " + subscriberId);
                break;

            case "PARKING_RESERVATION_FAILED_EARLY_ARRIVE":
                client.sendToClient("SubscriberTermenalController ParkWithReservation Failed Early arrival! Please wait.");
                EchoServer.messageController.appendMessage("Early arrival attempt by subscriber: " + subscriberId);
                break;

            default:
                sendToClientLabelUpdate(client, "Failed Unknown error - " + result);
                break;
        }
    }

    /**
     * Shuts down the server gracefully.
     * Closes the background database connection, disconnects all clients, and closes the server socket.
     * Appends appropriate shutdown messages to the server log.
     */
    public void shutdownServer() {
        try {
            if (serverBackgroundConnection != null && !serverBackgroundConnection.isClosed()) {
                serverBackgroundConnection.close();
                System.out.println("Server background connection closed.");
            }
        } catch (SQLException e) {
        	 EchoServer.messageController.appendMessage("Error while closing background connection: " + e.getMessage());
        }

        try {
            disconnectAllClients(); // Disconnect all connected clients
            close(); // Call to AbstractServer.close()
            messageController.appendMessage(" Server has been shut down. All clients disconnected.");
        } catch (IOException e) {
            messageController.appendMessage(" Error while shutting down server: " + e.getMessage());
        }
    }
    /**
     * Checks the parking duration for all currently parked subscribers.
     * If the parking time has exceeded the allowed duration, sends an email notification to the subscriber.
     * 
     * @param con the database connection used for the background query
     * @throws InterruptedException if the background operation is interrupted
     */
    public void checkForLateParkings(Connection con) throws InterruptedException {
        db.getOverdueParkingsInBackground(con, parkings -> {
            for (Map<String, String> record : parkings) {
                try {
                    String subscriberId = record.get("SubscriberID");
                    String email = record.get("Email");
                    String name = record.get("UserName");
                    String entryTimeStr = record.get("EntryTime");

                    int timeAllowedMinutes = Integer.parseInt(record.get("TimeToPark"));

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    LocalDateTime entryTime = LocalDateTime.parse(entryTimeStr, formatter);
                    LocalDateTime now = LocalDateTime.now();

                    // Calculate the allowed end time by adding the allowed minutes to the entry time
                    LocalDateTime allowedUntil = entryTime.plusMinutes(timeAllowedMinutes);

                    EchoServer.messageController.appendMessage(
                        "Checking subscriber " + subscriberId +
                        " | EntryTime: " + entryTime +
                        " | Allowed Until: " + allowedUntil +
                        " | Now: " + now
                    );

                    if (allowedUntil.isBefore(now)) {
                        EchoServer.messageController.appendMessage("Time expired - sending email to: " + email);
                        EmailSender.sendLatePickupEmail(email, name);
                    } else {
                        EchoServer.messageController.appendMessage(
                            "Subscriber " + subscriberId + " is still within allowed parking time."
                        );
                    }

                } catch (DateTimeParseException e) {
                    EchoServer.messageController.appendMessage("Invalid datetime format: " + record.get("EntryTime"));
                } catch (Exception e) {
                    EchoServer.messageController.appendMessage(
                        "Error for subscriber " + record.getOrDefault("SubscriberID", "UNKNOWN") +
                        ": " + e.getMessage()
                    );
                }
            }
        });
    }

    /**
     * Handles the update of a subscribers personal information (email and phone number).
     * Notifies the appropriate controller on the client side with success or failure.
     *
     * @param client         the client connection initiating the request
     * @param SubscriberID   the ID of the subscriber whose data should be updated
     * @param destinationPage the name of the page/controller to respond to (e.g., "Away_Page")
     * @param email          the new email address
     * @param phoneNumber    the new phone number
     * @throws InterruptedException if the update operation is interrupted
     */
    private void handleUpdatePersonalData(ConnectionToClient client, String SubscriberID, String destinationPage, String email, String phoneNumber) throws InterruptedException {
        messageController.appendMessage("Updating personal data of: " + SubscriberID);
        boolean success = db.updateSubscriberPersonalData(SubscriberID, email, phoneNumber);

        try {
            if (success) {
                if ("Away_Page".equals(destinationPage)) {
                    client.sendToClient("SubscriberAwayController UPDATE_SUCCESS");
                }
            } else {
                if ("Away_Page".equals(destinationPage)) {
                    client.sendToClient("SubscriberAwayController UPDATE_FAIL");
                }
            }
        } catch (IOException e) {
            messageController.appendMessage("Failed to send update result to client: " + e.getMessage());
        }
    }

    /**
     * Retrieves personal data of a subscriber and sends it back to the client.
     *
     * @param client          the client requesting the data
     * @param SubscriberID    the ID of the subscriber whose data is being requested
     * @param destinationPage the name of the page/controller to respond to (e.g., "Away_Page")
     * @throws InterruptedException if the fetch operation is interrupted
     */
    private void handleGetPersonalData(ConnectionToClient client, String SubscriberID, String destinationPage) throws InterruptedException {
        messageController.appendMessage("Getting personal data of: " + SubscriberID);
        String sendPersonalData = "";
        String PersonalData = db.getSubscriberPersonalData(SubscriberID);

        if ("Away_Page".equals(destinationPage)) {
            sendPersonalData = "SubscriberAwayController SHOW_PERSONAL_DATA " + PersonalData;
        }

        try {
            client.sendToClient(sendPersonalData);
        } catch (IOException e) {
            messageController.appendMessage("Failed to send personal data: " + e.getMessage());
        }
    }

    /**
     * Retrieves the parking history for a specific subscriber and sends it to the client.
     *
     * @param client          the client requesting the history
     * @param SubscriberID    the ID of the subscriber
     * @param destinationPage the destination page/controller requesting the history
     * @throws InterruptedException if the operation is interrupted
     */
    private void handleGetHistoryBySpecificSubscriber(ConnectionToClient client, String SubscriberID, String destinationPage) throws InterruptedException {
        messageController.appendMessage("Getting history for subscriber: " + SubscriberID);
        String sendHistory;
        String history = db.getHistoryBySubscriberID(SubscriberID);

        if ("Termenal_Page".equals(destinationPage)) {
            sendHistory = "SubscriberTermenalController SHOW_SUBSCRIBER_HISTORY " + history;
        } else {
            sendHistory = "SubscriberAwayController SHOW_SUBSCRIBER_HISTORY " + history;
        }

        try {
            client.sendToClient(sendHistory);
        } catch (IOException e) {
            messageController.appendMessage("Failed to send history: " + e.getMessage());
        }
    }
    
    /**
     * Processes a reservation request from a subscriber. Parses the requested start time and duration,
     * attempts to reserve a parking spot, and sends the result back to the client.
     *
     * @param client      the client requesting the reservation
     * @param dateTimeStr the requested reservation date and time in the format "yyyy-MM-dd HH mm"
     * @param duration    the duration in minutes for which the reservation is requested
     */
    private void Reserve(ConnectionToClient client, String dateTimeStr, String duration) {
        try {
            String response;
            messageController.appendMessage("Date and Time input: " + dateTimeStr);

            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH mm");
            LocalDateTime localDateTime = LocalDateTime.parse(dateTimeStr, inputFormatter);
            Timestamp requestedStartTime = Timestamp.valueOf(localDateTime);
            int longtime = Integer.parseInt(duration);
            Timestamp requestedEndTime = new Timestamp(requestedStartTime.getTime() + longtime * 60 * 1000L);
            LocalDateTime now = LocalDateTime.now();
            if(localDateTime.isBefore(now.plusHours(24))||localDateTime.isAfter(now.plusDays(7)))
            {
            	messageController.appendMessage("Reservation must be at least 24 hours from now and within 7 days.");
            	response = "SubscriberAwayController RESERVE_TIME_ERROR";
            	client.sendToClient(response);
            	return;
            }
            messageController.appendMessage("Making DB reservation...");
           
            String subscriberId = this.securityArray.get(client)[1];
            String result = db.reserve(subscriberId, requestedStartTime, requestedEndTime);
            messageController.appendMessage("Reserve result: " + result);

     
            if (result.startsWith("1 ")) {
                String reservationId = result.split(" ")[1];
                response = "SubscriberAwayController RESERVE_SUCCESS " + reservationId;
            } else {
                switch (result) {
                    case "0":
                        response = "SubscriberAwayController RESERVE_TIME_FAIL";
                        break;
                    case "DUPLICATE_DATE":
                        response = "SubscriberAwayController RESERVE_Exist";
                        break;
                    default:
                        response = "SubscriberAwayController RESERVE_FAIL";
                        break;
                }
            }

            client.sendToClient(response);

        } catch (DateTimeParseException e) {
            messageController.appendMessage("Invalid date format: " + dateTimeStr);
            try {
                client.sendToClient("SubscriberAwayController RESERVE_FAIL");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } catch (Exception e) {
            messageController.appendMessage("Error in Reserve: " + e.getMessage());
            try {
                client.sendToClient("SubscriberAwayController RESERVE_FAIL");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    /**
     * Handles the case where a subscriber has forgotten their parking code.
     * Retrieves the code from the database and sends it to the subscriber's email.
     * Also responds to the client with the appropriate status.
     *
     * @param client the client requesting the parking code
     */
    private void ForgotCode(ConnectionToClient client) {
        try {
            String subscriberId = this.securityArray.get(client)[1];
            String result = db.SendCode(subscriberId);

            String response;
            if (result.startsWith("1")) {
                String[] parts = result.split(" ");
                if (parts.length >= 4) {
                    String historyId = parts[1];
                    String email = parts[2];
                    String name = String.join(" ", Arrays.copyOfRange(parts, 3, parts.length));

                    EmailSender.sendParkingCodeEmail(email, name, historyId);
                    messageController.appendMessage("Code " + historyId + " sent to " + email);
                    response = "SubscriberTermenalController GET_PARKING_CODE_SUCCESS " + historyId;
                } else {
                    response = "SubscriberTermenalController Error FailedToRetrieveCode";
                }
            } else {
                response = "SubscriberTermenalController GET_PARKING_CODE_WARNING You/have/no/active/parking/session";
            }

            client.sendToClient(response);

        } catch (Exception e) {
            messageController.appendMessage("Error in ForgotCode: " + e.getMessage());
            try {
                client.sendToClient("SubscriberTermenalController Error InternalError");
            } catch (IOException ioException) {
                messageController.appendMessage("Failed to send fallback error to client.");
            }
        }
    }

    /**
     * Handles the request to add a new subscriber. Validates and inserts the data into the database,
     * then responds to the client with success or failure message.
     *
     * @param client  the client that requested to add a new subscriber
     * @param message the subscriber details as a list of string fields
     * @throws InterruptedException if the operation is interrupted
     */
    private void handleAddNewSubscriber(ConnectionToClient client, ArrayList<String> message) throws InterruptedException {
        try {
            messageController.appendMessage("Trying to add subscriber...");
            String addSubflag = db.addNewSubscriber(message);

            switch (addSubflag) {
                case "ERROR_EXISTS":
                    client.sendToClient("UsherFrameController ADD_SUB_FAILED_EXISTS " + message.get(0));
                    messageController.appendMessage("Subscriber already exists: " + message.get(0));
                    break;

                case "ERROR_INSERT":
                    client.sendToClient("UsherFrameController ADD_SUB_FAILED_INSERT " + message.get(0));
                    messageController.appendMessage("Insert failed for subscriber: " + message);
                    break;

                default:
                    // Success - result is the new subscriber ID (e.g., "SUB123")
                    client.sendToClient("UsherFrameController ADD_SUB_SUCCESSFULLY " + addSubflag);
                    messageController.appendMessage("Subscriber added successfully with ID: " + addSubflag);
                    break;
            }
        } catch (IllegalArgumentException e) {
            sendToClientLabelUpdate(client, e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Handles the request to add a Tag To subscriber. Validates and inserts the data into the database,
     * then responds to the client with success or failure message.
     *
     * @param client  the client (usher) that requested to add a new Tag to subscriber
     * @param message the subscriber details as a list of string fields
     * @throws InterruptedException if the operation is interrupted
     */
    private void AddTagToSubscriber(ConnectionToClient client, ArrayList<String> message) throws InterruptedException {
        try {
            messageController.appendMessage("Trying to add tag to subscriber "+message.toString());
            String addSubflag = db.addTagToSubscriber(message);
           
            switch (addSubflag) {
                case "ERROR_NO_SUCH_SUBSCRIBER":
                    client.sendToClient("UsherFrameController ERROR_NO_SUCH_SUBSCRIBER " + message.get(0));
                    
                    break;

                case "ERROR_INSERT_TAGREADER":
                    client.sendToClient("UsherFrameController ERROR_NO_SUCH_SUBSCRIBER " + message.get(0));
                    
                    break;

                case "ERROR_SQL":
                    client.sendToClient("UsherFrameController ADD_SUB_FAILED_INSERT " + message.get(0));
            
                    break;
                    
                case "ERROR_SUBSCRIBER_ALREADY_HAS_TAG":
                    client.sendToClient("UsherFrameController ERROR_SUBSCRIBER_ALREADY_HAS_TAG " + message.get(0));
                    break;

                default:
                    // Success 
                    client.sendToClient("UsherFrameController " + addSubflag);
                    messageController.appendMessage(addSubflag);
                    break;
            }
        } catch (IllegalArgumentException e) {
            sendToClientLabelUpdate(client, e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the request to Show history To usher for specific subscriber by ID. Validates and gets the data from the database,
     * then responds to the client with success or failure message.
     *
     * @param client  the client (usher) that requested to see history for subscriber
     * @param message the subscriber details as a list of string fields
     * @throws InterruptedException if the operation is interrupted
     */
    private void ShowSubscriberHistory(ConnectionToClient client, ArrayList<String> message) throws InterruptedException {
        try {
                    String addSubflag = db.getHistoryBySubscriberID(message.get(0));
                    client.sendToClient("UsherFrameController "+ "ShowHistory "+ addSubflag);
                    messageController.appendMessage("Showing History Activity for User:"+message.get(0));
        } catch (IllegalArgumentException e) {
            sendToClientLabelUpdate(client, e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Handles the request to retrieve all subscribers. Checks user type and responds with appropriate
     * controller tag (Manager or Usher) and subscriber data.
     *
     * @param client the client requesting the list of all subscribers
     * @throws InterruptedException if the operation is interrupted
     */
    private void handleGetAllSubscribers(ConnectionToClient client) throws InterruptedException {
        try {
            messageController.appendMessage("Getting All Subscribers: ");
            ArrayList<String> allSubscribersInfo = db.getAllSubscribers();

            if (allSubscribersInfo == null || allSubscribersInfo.isEmpty()) {
            	if ("Usher".equals(isClientOfType(client))) {
            		client.sendToClient("UsherFrameController SUBSCRIBERS_VIEW There is no subscribers found!");
            	}
                if ("Manager".equals(isClientOfType(client))) {
                	sendToClientLabelUpdate(client, "There is no subscribers found!");
                }
                messageController.appendMessage("There is no subscribers found!");
                return;
            } else {
                messageController.appendMessage(allSubscribersInfo.toString());
                ArrayList<String> response = new ArrayList<>();

                if ("Manager".equals(isClientOfType(client))) {
                    response.add("ManagerFrameController");
                    response.add("SUBSCRIBERS_VIEW");
                } else if ("Usher".equals(isClientOfType(client))) {
                    response.add("UsherFrameController");
                    response.add("SUBSCRIBERS_VIEW");
                }

                response.addAll(allSubscribersInfo);
                String result = String.join(" ", response);
                client.sendToClient(result);
            }
        } catch (IllegalArgumentException e) {
            sendToClientLabelUpdate(client, e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles a request to retrieve all active parking spots currently in use.
     * Sends the result back to the client based on their user type (Manager or Usher).
     *
     * @param client the client requesting active parking spot information
     * @throws InterruptedException if the operation is interrupted
     */
    private void handleActivParkingspot(ConnectionToClient client) throws InterruptedException {
        try {
            messageController.appendMessage("Generating GET_ACTIVE_PARKINGSPOT");
            String activeParkingData = db.getActiveParkingSpots();

            if (activeParkingData == null || activeParkingData.trim().isEmpty()) {
            	if ("Usher".equals(isClientOfType(client))) {
            		client.sendToClient("UsherFrameController SHOW_ACTIVE_PARKINGSPOT There is no active parking spots found! ");
            	}
            	if ("Manager".equals(isClientOfType(client))) {
            		sendToClientLabelUpdate(client, "No active parking spots found.");
            	}
                return;
            }

            if ("Manager".equals(isClientOfType(client))) {
                String messageToClient = "ManagerFrameController SHOW_ACTIVE_PARKINGSPOT " + activeParkingData;
                sendToClientLabelUpdate(client, messageToClient);
            } else if ("Usher".equals(isClientOfType(client))) {
                ArrayList<String> response = new ArrayList<>();
                response.add("UsherFrameController");
                response.add("SHOW_ACTIVE_PARKINGSPOT");
                response.add(activeParkingData);
                String result = String.join(" ", response);
                client.sendToClient(result);
            }
        } catch (IllegalArgumentException e) {
            sendToClientLabelUpdate(client, e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the request to check how many parking spots are currently available.
     * The response is sent to the corresponding controller based on the source page.
     *
     * @param client         the client making the request
     * @param controllerName the controller identifier ("Away" or "Termenal") to direct the result to
     * @throws InterruptedException if the operation is interrupted
     */
    private void handleCheckAvilableSpots(ConnectionToClient client, String controllerName) throws InterruptedException {
        try {
            messageController.appendMessage("Checking available spots for Subscriber: " + client.toString());
            String abilableSpots = db.COUNTEmptySpot();

            if (abilableSpots == null || abilableSpots.isEmpty()) {
                if (controllerName.equals("Away"))
                    client.sendToClient("SubscriberAwayController CHECK_SPOTS_FAIL");
                else
                    client.sendToClient("SubscriberTermenalController CHECK_SPOTS_FAIL");
            } else {
                messageController.appendMessage("Checking available spots succeeded.\n");
                if (controllerName.equals("Away"))
                    client.sendToClient("SubscriberAwayController CHECK_SPOTS_SUCCESS " + abilableSpots);
                else
                    client.sendToClient("SubscriberTermenalController CHECK_SPOTS_SUCCESS " + abilableSpots);
            }
        } catch (IllegalArgumentException e) {
            sendToClientLabelUpdate(client, e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the generation of a new parking code for a subscriber.
     * Ensures the parking time does not exceed the system limit, then requests a code from the database.
     *
     * @param client      the client requesting a parking code
     * @param timeToPark  the duration in minutes the subscriber wishes to park
     * @throws InterruptedException if the operation is interrupted
     */
    private void handleGetNewParkingCode(ConnectionToClient client, String timeToPark) throws InterruptedException {
        try {
            messageController.appendMessage("Generating parking code for Subscriber: " + client.toString());
            int intTimeToPark = Integer.parseInt(timeToPark);

            if (intTimeToPark > 240) {
                intTimeToPark = 240; // Cap the max time to 4 hours
            }

            String subID = this.securityArray.get(client)[1];
            String newParkingCode = db.getParkingCode(intTimeToPark, subID);

            if (newParkingCode == null || newParkingCode.isEmpty()) {
                messageController.appendMessage("Parking code generation FAILED");
                client.sendToClient("SubscriberTermenalController GET_PARKING_CODE_FAIL");
            } else if (newParkingCode.equals("You/already/parked/your/car.")) {
                messageController.appendMessage("Parking code generation FAILED");
                client.sendToClient("SubscriberTermenalController GET_PARKING_CODE_WARNING " + newParkingCode);
            } else {
                client.sendToClient("SubscriberTermenalController GET_PARKING_CODE_SUCCESS " + newParkingCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles sign-in logic for a subscriber using a tag reader ID.
     * If the tagReaderId is valid, the user is authenticated and added to the security map.
     *
     * @param tagReaderId the tag reader's unique ID used for sign-in
     * @param client      the client attempting to sign in
     * @throws InterruptedException if the operation is interrupted
     */
    private void handleTagReaderSignIn(String tagReaderId, ConnectionToClient client) throws InterruptedException {
        try {
            String subscriberId = db.getSubscriberIdByTagReader(tagReaderId);
            if (subscriberId != null && !subscriberId.isEmpty()) {

                // Check if this subscriber is already connected
                boolean alreadyConnected = securityArray.values().stream()
                    .anyMatch(userInfo -> userInfo[0].equals("User") && userInfo[1].equals(subscriberId));

                if (alreadyConnected) {
                    messageController.appendMessage(subscriberId + " is already connected!\nTrying to connect from another device is disallowed!");
                    client.sendToClient("SignInController SIGN_IN_TWICE_LOGOUT");
                } else {
                    client.sendToClient("SignInController SIGN_IN_SUCCESS USERTermenal");
                    String[] u = {"User", subscriberId};
                    securityArray.put(client, u);
                }

            } else {
                client.sendToClient("SignInController SIGN_IN_Fail USERTermenal");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    
    /**
     * Sends a message to the client to update a label or status on their UI.
     * Prepends a space before the message to comply with the client-side protocol.
     *
     * @param client  the client to send the message to
     * @param message the message content
     */
    private void sendToClientLabelUpdate(ConnectionToClient client, String message) {
        try {
            client.sendToClient(" " + message);
        } catch (IOException e) {
            messageController.appendMessage("Failed to send error message to client: " + e.getMessage());
        }
    }

    /**
     * Safely casts an object to an ArrayList of Strings, verifying all elements are Strings.
     *
     * @param obj the object to cast
     * @return a valid ArrayList&lt;String&gt; if successful, otherwise null
     */
    public static ArrayList<String> safeCastToStringList(Object obj) {
        if (obj instanceof ArrayList<?>) {
            ArrayList<?> temp = (ArrayList<?>) obj;
            for (Object o : temp) {
                if (!(o instanceof String)) {
                    return null;
                }
            }
            ArrayList<String> result = new ArrayList<>();
            for (Object o : temp) {
                result.add((String) o);
            }
            return result;
        }
        return null;
    }

    /**
     * Main method to start the EchoServer.
     * Uses the default port unless a custom port is provided as a command-line argument.
     *
     * @param args optional command-line argument specifying the port
     */
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        EchoServer server = new EchoServer(port);
        try {
            server.listen();
        } catch (Exception ex) {
            messageController.appendMessage("ERROR - Could not listen for clients!");
        }
    }

    /**
     * Starts a background thread that monitors client inactivity.
     * If a client has been inactive for over 1 hour, they are notified and disconnected.
     * The thread runs continuously and checks for inactivity every 60 seconds.
     */
    private void startInactivityMonitor() {
        Thread inactivityThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(60000); // Check every 60 seconds
                        long now = System.currentTimeMillis();
                        List<ConnectionToClient> toRemove = new ArrayList<>();

                        for (Map.Entry<ConnectionToClient, Long> entry : lastActivity.entrySet()) {
                            if (now - entry.getValue() > 3600000) { // Inactive for over 1 hour
                                ConnectionToClient client = entry.getKey();

                                sendToClientLabelUpdate(client, "Disconnected due to inactivity ");
                                messageController.appendMessage("Disconnecting inactive client: " + client.getInetAddress());

                                Thread.sleep(3000); // Give client 3 seconds to read the message

                                client.close(); // Disconnect
                                toRemove.add(client);
                            }
                        }

                        for (ConnectionToClient client : toRemove) {
                            lastActivity.remove(client);
                            updateClientStatus(null); // Optional UI refresh
                        }

                    } catch (InterruptedException e) {
                        break; // Thread interrupted, exit safely
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        inactivityThread.start(); // Start the thread
    }
   
    /**
     * Handles user sign-in from either a terminal or remote (away) location.
     * If the subscriber ID and name are valid, stores the session in the security array
     * and notifies the client of successful login.
     *
     * @param subscriberId   the subscriber's ID
     * @param subscriberName the subscriber's name
     * @param client         the client attempting to sign in
     * @param TypeUser       indicates the login context ("Termenal" or "Away")
     * @throws InterruptedException if the operation is interrupted
     */
    private void handleUserSignIn(String subscriberId, String subscriberName, ConnectionToClient client, String TypeUser) throws InterruptedException {
        try {
            messageController.appendMessage("Getting Subscriber: " + subscriberId);
            String subscriberInfo = db.getSubscriberID(subscriberId, subscriberName);

            if (subscriberInfo == null || subscriberInfo.isEmpty()) {
                client.sendToClient("SignInController SIGN_IN_Fail USER");
                return;
            }

            // If it's a terminal user, check if already connected
            if (TypeUser.equals("Termenal")) {
                boolean alreadyConnected = securityArray.values().stream()
                    .anyMatch(userInfo -> userInfo[0].equals("User") && userInfo[1].equals(subscriberId));

                if (alreadyConnected) {
                    messageController.appendMessage(subscriberId + " is already connected!\nTrying to connect from another device is disallowed!");
                    client.sendToClient("SignInController SIGN_IN_TWICE_LOGOUT");
                    return;
                }
                
            }
            
	            // Sign-in success
	            messageController.appendMessage(subscriberInfo);
	            if (TypeUser.equals("Termenal")) {
	            	String[] u = {"User", subscriberId};
	            	securityArray.put(client, u);
	            }
	            else
	            {
	            	String[] u = {"UserAway", subscriberId};
	            	securityArray.put(client, u);
	            }
	            client.sendToClient("SignInController SIGN_IN_SUCCESS USER" + TypeUser);
            
        } catch (IllegalArgumentException e) {
            sendToClientLabelUpdate(client, e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Handles worker sign-in based on their ID and type (0 for Usher, 1 for Manager).
     * On success, adds the worker type to the security array and notifies the client.
     *
     * @param workerId the worker's ID
     * @param type     the type of worker ("0" for Usher, "1" for Manager)
     * @param client   the client attempting to sign in
     * @throws InterruptedException if the operation is interrupted
     */
    private void handleWorkerSignIn(String workerId, String type, ConnectionToClient client) throws InterruptedException {
        try {
            messageController.appendMessage("Getting Worker: " + workerId);
            String workerInfo = db.getWorkerID(workerId, type);

            if (workerInfo == null || workerInfo.isEmpty()) {
                client.sendToClient("SignInController SIGN_IN_Fail Worker " + type);
                return;
            }

            // Check if this workerId is already logged in
            boolean alreadyConnected = securityArray.values().stream()
                .anyMatch(userInfo -> userInfo[1].equals(workerId));

            if (alreadyConnected) {
                messageController.appendMessage(workerId + " is already connected!\nTrying to connect from another device is disallowed!");
                client.sendToClient("SignInController SIGN_IN_TWICE");
            } else {
                String role = "0".equals(type) ? "Usher" : "Manager";
                String[] u = {role, workerId};
                securityArray.put(client, u);
                messageController.appendMessage(workerInfo);
                client.sendToClient("SignInController SIGN_IN_SUCCESS Worker " + type);
            }

        } catch (IllegalArgumentException e) {
            sendToClientLabelUpdate(client, e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Handles a manager's request to fetch a subscriber's visual report.
     * Converts the date, retrieves the image from the database, and sends it to the client.
     * If the image doesn't exist, notifies the client with a status message.
     *
     * @param subscriberId the subscriber's ID
     * @param dateStr the report date as a string (YYYY-MM-DD)
     * @param client the requesting manager client
     */
    private void handleSubscriptionReport(String subscriberId, String dateStr, ConnectionToClient client) throws InterruptedException {
        try {
            Date reportDate = Date.valueOf(dateStr);
            messageController.appendMessage("Fetching SUBSCRIPTION_REPORT image for subscriber: " + subscriberId + " at date: " + reportDate);

            byte[] reportImage = db.getSubscriptionReport(subscriberId, reportDate);

            if (reportImage != null) {
                MyFile reportFile = new MyFile("ManagerFrameController SHOW_SUBSCRIPTION_REPORT", reportImage);
                client.sendToClient(reportFile);
                messageController.appendMessage("Subscription report image sent to client.");
            } else {
                sendToClientLabelUpdate(client, "No subscription report image found for this subscriber and date.");
            }

        } catch (IllegalArgumentException e) {
            sendToClientLabelUpdate(client, "Invalid date format for subscription report.");
        } catch (IOException e) {
            messageController.appendMessage("Failed to send subscription report image: " + e.getMessage());
        }
    }
    
    /**
     * Determines the type of the connected client (e.g., User, Usher, Manager).
     *
     * @param client the client whose type is to be checked
     * @return the role string associated with the client ("User", "Usher", "Manager"), or null if not found
     */
    public String isClientOfType(ConnectionToClient client) {
        if (!securityArray.containsKey(client)) {
            return null;
        }
        String[] role = securityArray.get(client);
        return role[0];
    }

    /**
     * Handles a request from a subscriber to check their current parking status.
     * Sends the status to the client if found, or an empty status notification if not.
     *
     * @param client the subscriber client requesting the parking status
     */
    public void handleGetMyParkingStatus(ConnectionToClient client) {
        try {
            messageController.appendMessage("Checking Parking status for Subscriber : " + client.toString());

            String subscriberId = this.securityArray.get(client)[1];
            String ParkingStatus = db.GetSubscriberCurrentParkingStatus(subscriberId);

            if (ParkingStatus == null || ParkingStatus.isEmpty()) {
                client.sendToClient("SubscriberTermenalController GOT_EMPTY_STATUS");
            } else {
                messageController.appendMessage("Checking Parking status succeeded.\n");
                client.sendToClient("SubscriberTermenalController GET_PARKING_STATUS_SUCCESS " + ParkingStatus);
            }
        } catch (IllegalArgumentException e) {
            sendToClientLabelUpdate(client, e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles a request from a subscriber to retrieve their parked car.
     * Checks if the subscriber has an active parking session, calculates if they are late,
     * and updates the database accordingly.
     *
     * @param client     the subscriber client requesting to retrieve the car
     * @param parkingID  the ID of the parking spot or session
     */
    public void handleRetrieveCar(ConnectionToClient client, String parkingID) {
        try {
            messageController.appendMessage("Retrieving car for Subscriber : " + client.toString());

            String subscriberId = this.securityArray.get(client)[1];
            String ParkingStatus = db.GetSubscriberCurrentParkingStatus(subscriberId);

            if (ParkingStatus == null || ParkingStatus.isEmpty()) {
                messageController.appendMessage("The subscriber does not have a car to retrieve");
                client.sendToClient("SubscriberTermenalController NO_CAR_TO_RETRIEVE");
            } else {
                String[] stats = ParkingStatus.split(" ");
                String StartTime = stats[1] + " " + stats[2];
                int maxTimeInMin = Integer.parseInt(stats[3]);

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime pastDateTime = LocalDateTime.parse(StartTime, formatter);
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime lateThreshold = now.minusMinutes(maxTimeInMin);
                Duration duration = Duration.between(lateThreshold, pastDateTime);

                long totalParkingTime = Math.abs(Duration.between(now, pastDateTime).toMinutes());
                String isLate = duration.isNegative() ? "1" : "0";

                boolean res = db.retriveCarForSubscriber(subscriberId, stats[0], isLate, String.valueOf(totalParkingTime), parkingID);

                if (res) {
                    messageController.appendMessage("Retrieving car succeeded.\n");
                    client.sendToClient("SubscriberTermenalController RETRIEVING_CAR_SUCCESS");
                    handleGetMyParkingStatus(client);
                } else {
                    messageController.appendMessage("Retrieving car failed.\n");
                    client.sendToClient("SubscriberTermenalController RETRIEVING_CAR_FIALED");
                }
            }
        } catch (IllegalArgumentException e) {
            sendToClientLabelUpdate(client, e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @author Amit_Regev
     * Generates the monthly parking report for the previous month.
     * This includes: total parking time in hours, number of late exits, number of extensions,
     * and the most requested hour during the month.
     * A visual bar chart is generated and saved as a BLOB into the database.
     * Ensures the report is generated only once per month, even if triggered multiple times.
     */
    private void generateMonthlyParkingReportAutomatically(Connection con) {
        // Step 1: determine date range
        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        LocalDate from = previousMonth.atDay(1);             // 2025-05-01
        LocalDate to = previousMonth.plusMonths(1).atDay(1); // 2025-06-01

        // Step 2: check duplication
        if (previousMonth.equals(lastMonthlyReportMonth)) {
            messageController.appendMessage("Monthly report for " + previousMonth + " has already been generated.");
            return;
        }

        messageController.appendMessage("Generating Monthly Parking Report for: " + previousMonth);

        // Step 3: fetch raw statistics
        Map<String, String> rawStats = db.getMonthlyParkingStatsRaw(from, to,con);
        if (rawStats != null && !rawStats.isEmpty()) {
            int minutes = Integer.parseInt(rawStats.getOrDefault("TotalMinutes", "0"));
            int hours = minutes / 60;
            int lateExits = Integer.parseInt(rawStats.getOrDefault("LateExits", "0"));
            int ext = Integer.parseInt(rawStats.getOrDefault("Extensions", "0"));
            int reservationCount = Integer.parseInt(rawStats.getOrDefault("ReservationCount", "0"));
            int cancelledReservations = Integer.parseInt(rawStats.getOrDefault("CancelledReservations", "0"));
            int lateReservationArrivals = Integer.parseInt(rawStats.getOrDefault("LateReservationArrivals", "0"));
            String peak = rawStats.getOrDefault("MostRequestedHour", "N/A");
            if (!peak.equals("N/A")) {
                peak = String.format("%02d:00", Integer.parseInt(peak));
            }

            String log = String.format(
            	    "Monthly Report: %d total minutes, %d late exits, %d extensions, %d reservations, %d cancellations, %d late arrivals from reservations, Peak hour: %s",
            	    minutes, lateExits, ext, reservationCount, cancelledReservations, lateReservationArrivals, peak
            	);
            	messageController.appendMessage(log);
            final String peakHour = peak;
            // Step 4: create and save chart image
            Platform.runLater(() -> {
            	byte[] chartImageBytes = createMonthlyParkingChartImage(minutes, lateExits, ext, reservationCount, cancelledReservations, lateReservationArrivals, peakHour);
                if (chartImageBytes != null) {
                    LocalDate reportDate = previousMonth.plusMonths(1).atDay(1); // e.g. 2025-07-01
                    try {
                        db.saveParkingReportImage(reportDate, chartImageBytes,con);
                        messageController.appendMessage("Chart image saved to database for " + previousMonth);
                    } catch (InterruptedException e) {
                        messageController.appendMessage("Thread was interrupted while saving chart image: " + e.getMessage());
                        Thread.currentThread().interrupt();
                    }
                }
            });

        }


        lastMonthlyReportMonth = previousMonth;
    }
    
    /**
     * Generates and stores a visual monthly report image for all subscribers.
     * Fetches all parking history for the previous month in a single query,
     * groups it by SubscriberID, and creates a chart image for each subscriber.
     * The image is stored in the 'subscriberreport' table as a BLOB.
     */
    public void generateAllSubscriberReportsAutomatically(Connection con) {
        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        LocalDate from = previousMonth.atDay(1);
        LocalDate to = previousMonth.plusMonths(1).atDay(1);
        LocalDate reportDate = to;

        messageController.appendMessage("Generating all subscriber reports for " + previousMonth);

        try {
            List<Map<String, String>> rawHistory = db.getFullHistoryForMonth(from, to,con);
            Map<String, List<Map<String, String>>> groupedBySubscriber = new HashMap<>();

            for (Map<String, String> row : rawHistory) {
                String subscriberId = row.get("SubscriberID");
                messageController.appendMessage("Generated report for subscriber: " + subscriberId);

                groupedBySubscriber.computeIfAbsent(subscriberId, k -> new ArrayList<>()).add(row);
            }

            // Execute GUI and chart generation safely in JavaFX thread
            Platform.runLater(() -> {
                try {
                    List<Map<String, Object>> batchData = new ArrayList<>();

                    for (Map.Entry<String, List<Map<String, String>>> entry : groupedBySubscriber.entrySet()) {
                        String subscriberId = entry.getKey();
                        List<Map<String, String>> records = entry.getValue();
                        int totalParkings = records.size();
                        int lateArrivals = 0;
                        int lateLeaves = 0;
                        int lateExits = 0;
                        int extensions = 0;
                        int reservationCount = 0;
                        int cancelledReservations = 0;
                        int lateFromReservation = 0;
                        int totalMinutes = 0;


                        for (Map<String, String> rec : records) {
                            if ("1".equals(rec.get("Late"))) {
                                lateArrivals++;
                            }

                            if ("1".equals(rec.get("LateEmailSent"))) {
                                lateExits++;  
                            }

                            if ("1".equals(rec.get("Extensions"))) {
                                extensions++;
                            }

                            String resId = rec.get("resID");
                            if (resId != null && !resId.equalsIgnoreCase("null")) {
                                reservationCount++;
                                if ("0".equals(rec.get("ShowedUp"))) {
                                    cancelledReservations++;
                                }
                                if ("1".equals(rec.get("Late"))) {
                                    lateFromReservation++;
                                }
                            }

                            LocalDateTime entryTime = LocalDateTime.parse(rec.get("EntryTime"));
                            LocalDateTime exitTime = LocalDateTime.parse(rec.get("ExitTime"));
                            int duration = (int) java.time.Duration.between(entryTime, exitTime).toMinutes();
                            totalMinutes += duration;
                        }


                        byte[] imageBytes = createSubscriberChartImage(lateFromReservation, lateExits, totalMinutes, extensions, reservationCount, cancelledReservations);


                        Map<String, Object> reportEntry = new HashMap<>();
                        reportEntry.put("SubscriberID", subscriberId);
                        reportEntry.put("ImageBytes", imageBytes);
                        batchData.add(reportEntry);
                    }

                    boolean success = db.saveSubscriberReportsBatch(reportDate, batchData,con);
                    if (success) {
                        messageController.appendMessage("All subscriber reports saved for: " + previousMonth);
                    } else {
                        messageController.appendMessage("Failed to save some subscriber reports.");
                    }
                } catch (Exception e) {
                    messageController.appendMessage("Batch save error: " + e.getMessage());
                }
            });

        } catch (Exception e) {
            messageController.appendMessage("Failed generating all subscriber reports: " + e.getMessage());
        }
    }
    /**
     * After Customer is late 4 hours! the car must be out for other reservations!
     * We force the car to exit 
     * We check that EntryTime+TimeToPark>=EntryTime+TimeToPark+240(minutes)
     */
    public void ExitParkingCarsAfter4HOURS(Connection con) {
        db.ExitLateParkingsAfter4HOURS(con, parkings -> {
            for (Map<String, String> record : parkings) {
                try {
                    String subscriberId = record.get("SubscriberID");
                    String email = record.get("Email");
                    String name = record.get("UserName");
                    String exitTime = record.get("ExitTime");

                    EchoServer.messageController.appendMessage(
                        "Notifying subscriber " + subscriberId + " that their car was removed at " + exitTime
                    );

                    EmailSender.sendForcedExitEmail(email, name, exitTime);

                } catch (Exception e) {
                    EchoServer.messageController.appendMessage(
                        "Error sending email to subscriber " + record.getOrDefault("SubscriberID", "UNKNOWN") +
                        ": " + e.getMessage()
                    );
                    e.printStackTrace();
                }
            }
        });
    }

    
    /**
     * Generates a bar chart image representing the monthly parking stats.
     * Includes total hours, late exits, extensions, reservation metrics, and peak hour.
     * @author Amit_Regev
     * @param totalHours  Total hours parked
     * @param lateExits   Number of late exits
     * @param extensions  Number of extensions
     * @param reservations Number of reservation-based parkings
     * @param cancellations Number of reservations that were canceled (user didn't show up)
     * @param lateArrivalsFromReservations Number of late entries where there was a reservation
     * @param peakHour    Peak entry hour (e.g., "08:00")
     * @return byte[] of PNG image for storing in DB
     */
    public byte[] createMonthlyParkingChartImage(int totalMinutes, int lateExits, int extensions,
                                                 int reservations, int cancellations,
                                                 int lateArrivalsFromReservations, String peakHour) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Monthly Parking Report - Peak Hour: " + peakHour);
        xAxis.setLabel("Category");
        yAxis.setLabel("Count");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(createLabeledData("Total Minutes", totalMinutes));
        series.getData().add(createLabeledData("Late Exits", lateExits));
        series.getData().add(createLabeledData("Extensions", extensions));
        series.getData().add(createLabeledData("Reservations", reservations));
        series.getData().add(createLabeledData("Cancellations", cancellations));
        series.getData().add(createLabeledData("Late Entries w/ Reservation", lateArrivalsFromReservations));
        barChart.getData().add(series);


        // Render chart to image
        VBox box = new VBox(barChart);
        Scene scene = new Scene(box);
        barChart.applyCss(); // force rendering
        barChart.layout();

        WritableImage fxImage = scene.snapshot(null);
        BufferedImage img = SwingFXUtils.fromFXImage(fxImage, null);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    
    /**
     * Creates a chart data point with a label shown below the bar.
     * 
     * @param category the x-axis label
     * @param value    the numeric value for the bar
     * @return a Data object with custom node including the value label
     */
    private XYChart.Data<String, Number> createLabeledData(String category, int value) {
        XYChart.Data<String, Number> data = new XYChart.Data<>(category, value);

        Label valueLabel = new Label(String.valueOf(value));
        valueLabel.setStyle("-fx-font-size: 12; -fx-text-fill: black;");

        VBox box = new VBox();
        box.setSpacing(2);
        box.setAlignment(javafx.geometry.Pos.BOTTOM_CENTER);
        box.getChildren().addAll(new javafx.scene.shape.Rectangle(0, 0), valueLabel); // Transparent spacer
        data.setNode(box);

        return data;
    }
    
	/**
	 * Generates a bar chart image showing a subscriber's monthly parking statistics.
	 * The chart includes:
	 * - Number of late arrivals
	 * - Number of late departures (overstayed)
	 * - Total parking duration in minutes
	 * 
	 * Each bar is labeled with its exact numeric value for clarity, even when values differ significantly.
	 * 
	 * @param lateArrivals    Number of late entries by the subscriber
	 * @param lateLeaves      Number of overstayed exits by the subscriber
	 * @param totalDuration   Total parking duration in minutes
	 * @return PNG image (as byte array) representing the chart for storing in the database
	 */
    public byte[] createSubscriberChartImage(int lateFromReservation, int lateExits, int totalMinutes , int extensions, int reservationCount, int cancelledReservations ) {
    	CategoryAxis xAxis = new CategoryAxis();
    	NumberAxis yAxis = new NumberAxis();
    	BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
    	chart.setTitle("Subscriber Monthly Report");

    	xAxis.setLabel("Category");
    	yAxis.setLabel("Value");

    	XYChart.Series<String, Number> series = new XYChart.Series<>();

    	// Add data with custom nodes (label below)
    	series.getData().add(createLabeledData("Total Minutes", totalMinutes));
    	series.getData().add(createLabeledData("Late Exits", lateExits));
    	series.getData().add(createLabeledData("Extensions", extensions));
    	series.getData().add(createLabeledData("Reservations", reservationCount));
    	series.getData().add(createLabeledData("Cancellations", cancelledReservations));
    	series.getData().add(createLabeledData("Late from Reservation", lateFromReservation));

    	chart.getData().add(series);

    	VBox box = new VBox(chart);
    	Scene scene = new Scene(box);
    	chart.applyCss();
    	chart.layout();

    	WritableImage image = scene.snapshot(null);
    	BufferedImage buf = SwingFXUtils.fromFXImage(image, null);

    	try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
    		ImageIO.write(buf, "png", out);
    		return out.toByteArray();
    	} catch (IOException e) {
    		e.printStackTrace();
    		return null;
    	}
    }
    
    /**
     * Handles the request from the manager to fetch a visual parking report for a specific date.
     * If a chart image exists in the database for the given date, it is sent back to the client
     * wrapped inside a {@link MyFile} object with the command: "ManagerFrameController SHOW_PARKING_REPORT".
     *
     * @param dateStr The date (as String) selected by the manager (e.g., "2025-06-01").
     * @param client The client connection requesting the report.
     * @throws InterruptedException if the thread is interrupted while accessing the DB connection.
      */
    private void handleParkingReport(String dateStr, ConnectionToClient client) throws InterruptedException {
        try {
            Date reportDate = Date.valueOf(dateStr);
            messageController.appendMessage("Fetching PARKING_REPORT image for date: " + reportDate);

            byte[] reportImage = db.getParkingReportByDate(reportDate);

            if (reportImage != null) {
                MyFile reportFile = new MyFile("ManagerFrameController SHOW_PARKING_REPORT", reportImage);
                client.sendToClient(reportFile);
                messageController.appendMessage("Report image sent to client.");
            } else {
                sendToClientLabelUpdate(client, "No report image found for date: " + reportDate);
            }

        } catch (IllegalArgumentException e) {
            sendToClientLabelUpdate(client, "Invalid date format for parking report.");
        } catch (IOException e) {
            messageController.appendMessage("Failed to send report image: " + e.getMessage());
        }
    }
    
    /**
     * Handles checking whether the given subscriber currently has an active parking reservation.
     *@author Amit_Regev
     * @param client The client connection
     * @param subscriberID The ID of the subscriber
     * @param sourcePage The source page from which the request originated (e.g., "Away_Page")
     */
    private void handleCheckActiveParking(ConnectionToClient client, String subscriberID, String sourcePage) {
        try {
            if (db.hasActiveParking(subscriberID)) {
                sendToClientLabelUpdate(client, "ACTIVE_PARKING");
            } else {
                sendToClientLabelUpdate(client, "NO_ACTIVE_PARKING");
            }
        } catch (InterruptedException e) {
            sendToClientLabelUpdate(client, "Error while checking active parking.");
            EchoServer.messageController.appendMessage("InterruptedException in handleCheckActiveParking: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Handles a request to extend parking duration for a subscriber.
     * Calls the database logic to determine if extension is allowed and
     * sends the result back to the client.
     *
     * @param client        The client requesting the extension
     * @param subscriberID  The subscriber ID
     * @param totalMinutes  The requested extension time in minutes
     * @param sourcePage    The originating controller (e.g., "SubscriberAwayController")
     */
    private void handleExtendParkingTime(ConnectionToClient client, String subscriberID, int totalMinutes, String sourcePage) {
        try {
            messageController.appendMessage("handleExtendParkingTime called with subscriberID=" + subscriberID + ", totalMinutes=" + totalMinutes + ", sourcePage=" + sourcePage);

            String resultMessage = db.extendParkingDurationInDB(subscriberID, totalMinutes);
            messageController.appendMessage("DB returned: " + resultMessage);

            String fullMessage = sourcePage + " EXTEND_PARKING_RESULT " + resultMessage;
            messageController.appendMessage("Sending to client: " + fullMessage);

            sendToClientLabelUpdate(client, fullMessage);

        } catch (InterruptedException e) {
            messageController.appendMessage("InterruptedException in handleExtendParkingTime: " + e.getMessage());
            sendToClientLabelUpdate(client, sourcePage + " EXTEND_PARKING_RESULT ERROR: Interrupted operation.");
            Thread.currentThread().interrupt();

        } catch (SQLException e) {
            messageController.appendMessage("SQLException in handleExtendParkingTime: " + e.getMessage());
            sendToClientLabelUpdate(client, sourcePage + " EXTEND_PARKING_RESULT ERROR: Database error.");
        }
    }

} // End of EchoServer
