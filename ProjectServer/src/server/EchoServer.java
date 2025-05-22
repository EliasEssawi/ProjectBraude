package server;

import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gui.ServerMessageFrameController;
import javafx.application.Platform;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;

public class EchoServer extends AbstractServer {
	private final HashMap<ConnectionToClient, Long> lastActivity = new HashMap<>();

    public static final int DEFAULT_PORT = 5555;
    private mysqlConnection db;
    public static ServerMessageFrameController messageController;
    private ArrayList<String> DisconnectedIPs = new ArrayList<>();
    private int numOfClients = 0;

    public EchoServer(int port) {
        super(port);
        db = new mysqlConnection();
    }

    @Override
    protected void clientConnected(ConnectionToClient client) {
        String ip = client.getInetAddress().getHostAddress();
        DisconnectedIPs.add(ip);
        numOfClients++;
        updateClientStatus("Client connected: " + ip);
    }

    @Override
    protected synchronized void clientException(ConnectionToClient client, Throwable exception) {
        System.out.println("Client exception: " + exception.getMessage());
        lastActivity.remove(client);
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

    @Override
    protected synchronized void clientDisconnected(ConnectionToClient client) {
        lastActivity.remove(client);
        updateClientStatus(null);
    }

    @Override
    protected void serverStarted() {
        updateClientStatus("Server is now listening on port " + getPort());
        startInactivityMonitor(); 
    }
//maybe this need to be edited! 
    @Override
    protected void serverStopped() {
        Platform.runLater(() -> {
            messageController.appendMessage("Server has stopped listening for connections.");
        });
    }

    private void updateClientStatus(String msg) {
        Platform.runLater(() -> {
            if (msg != null) {
                messageController.appendMessage(msg);
            }
            messageController.appendMessage("Connected clients: " + getNumberOfClients());
            printConnectedIPs();
        });
    }

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

    @Override
    public void handleMessageFromClient(Object msg, ConnectionToClient client) {
    	lastActivity.put(client, System.currentTimeMillis());
    	int isParkUpdated = -9;
        int isTimeUpdated = -9;

        String clientIP = client.getInetAddress().getHostAddress();
        messageController.appendMessage("Client IP: " + clientIP);
        messageController.appendMessage("Message received: " + msg.toString() + " from " + client);

        ArrayList<String> messageList = safeCastToStringList(msg);
        if (messageList == null) {
            sendToClientLabelUpdate(client, "Oops, Something Went Wrong\nnumber of Data is wrong!");
            return;
        }

        if (messageList.size() == 1) {
            handleSingleMessage(messageList.get(0), client);
        } else if (messageList.size() == 3 && parseData(messageList, client)) {
            try {
                int parkingSpace = Integer.parseInt(messageList.get(0));
                int orderNumber = Integer.parseInt(messageList.get(1));
                Date orderDate = Date.valueOf(messageList.get(2));

                messageController.appendMessage("parking space :" + parkingSpace);
                messageController.appendMessage("orderNumber :" + orderNumber);
                messageController.appendMessage("orderDate :" + orderDate);

                if (!db.isDateValid(orderNumber, orderDate)) {
                    sendToClientLabelUpdate(client, "date should be within 24 hours to 7 days!");
                    return;
                }

                if (db.checkEquals(parkingSpace, orderDate, orderNumber)) {
                    sendToClientLabelUpdate(client, "Your Order is the same, nothing changed!");
                    return;
                }

                if (db.checkParkingSpaceIfExists(parkingSpace)) {
                    if (db.isDateTakenForParkingSpace(parkingSpace, orderDate, orderNumber)) {
                        sendToClientLabelUpdate(client, "parking space already taken\nchange parking space or date");
                        return;
                    }

                    isParkUpdated = db.updateParkingSpace(orderNumber, parkingSpace);
                    isTimeUpdated = db.updateOrderDate(orderNumber, orderDate);

                    if (isParkUpdated == 1 && isTimeUpdated == 1) {
                        sendToClientLabelUpdate(client, "Server: Data received and saved to database.");
                    } else if (isTimeUpdated == 1) {
                        sendToClientLabelUpdate(client, "Server: Date received and saved to database.");
                    } else if (isParkUpdated == 1) {
                        sendToClientLabelUpdate(client, "Server: Park Space received and saved to database.");
                    }
                } else {
                    sendToClientLabelUpdate(client, "Praking Space doesn't exist!");
                }
            } catch (Exception e) {
                e.printStackTrace();
                messageController.appendMessage("Exception: " + e.getMessage());
                sendToClientLabelUpdate(client, "Error processing data.");
            }
        } else {
            sendToClientLabelUpdate(client, "Oops, Something Went Wrong\nnumber of Data is wrong!");
        }
    }

    private void handleSingleMessage(String orderNumberStr, ConnectionToClient client) {
        try {
            int orderNumber = Integer.parseInt(orderNumberStr);
            if (db.orderExists(orderNumber)) {
                messageController.appendMessage("Found");
                String details = db.printOrderById(orderNumber);
                sendToClientSafely(client, details);
            } else {
                sendToClientLabelUpdate(client, "Order not found.");
            }
        } catch (NumberFormatException e) {
            sendToClientLabelUpdate(client, "Invalid order number format.");
        }
    }

    private boolean parseData(ArrayList<String> msg, ConnectionToClient client) {
        if (msg == null || msg.size() != 3) {
            sendToClientLabelUpdate(client, "Error: you left empty field!");
            return false;
        }
        try {
            Integer.parseInt(msg.get(0));
            Date.valueOf(msg.get(2));
        } catch (Exception e) {
            sendToClientLabelUpdate(client, "Error: Invalid data format.\n int for parking place, date for parking date (yyyy-mm-dd).");
            return false;
        }
        return true;
    }

    private void sendToClientSafely(ConnectionToClient client, String message) {
        try {
            client.sendToClient(message);
        } catch (IOException e) {
            messageController.appendMessage("Failed to send message to client: " + e.getMessage());
        }
    }

    private void sendToClientLabelUpdate(ConnectionToClient client, String message) {
        try {
            client.sendToClient("ORDER_ERROR: " + message);
        } catch (IOException e) {
            messageController.appendMessage("Failed to send error message to client: " + e.getMessage());
        }
    }

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
    private void startInactivityMonitor() {
        Thread inactivityThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                    	Thread.sleep(60000); // check every 60 seconds (1 minute)
                        long now = System.currentTimeMillis();
                        List<ConnectionToClient> toRemove = new ArrayList<>();

                        for (Map.Entry<ConnectionToClient, Long> entry : lastActivity.entrySet()) {
                            if (now - entry.getValue() >3600000) { // inactive for over 1 hour
                                ConnectionToClient client = entry.getKey();

                                //  Notify client before disconnect
                                sendToClientLabelUpdate(client, "Disconnected due to inactivity (60 seconds).");
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
                        break; // Thread was interrupted - safe exit
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        inactivityThread.start(); // 🚀 Start the thread
    }

} // End of EchoServer
