package server;

import java.sql.*;
import gui.ServerMessageFrameController;
import gui.ServerPortFrameController;

public class mysqlConnection {

    private Connection conn;

    // Constructor
    public mysqlConnection() {
        connectToDB();
    }

    // Connect to database
    private void connectToDB() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
        } catch (Exception ex) {
            System.out.println("Driver definition failed");
            return;
        }

        try {
            conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/parking?useSSL=false&serverTimezone=Asia/Jerusalem",
                    "root", "Ee030620");
            if (EchoServer.messageController != null) {
                EchoServer.messageController.appendMessage("SQL connection succeeded.");
            }
        } catch (SQLException ex) {
            EchoServer.messageController.appendMessage("SQLException: " + ex.getMessage());
            EchoServer.messageController.appendMessage("SQLState: " + ex.getSQLState());
            EchoServer.messageController.appendMessage("VendorError: " + ex.getErrorCode());
        }
    }

    // Reconnect if needed
    public Connection getConnection() {
        try {
            if (conn == null || conn.isClosed()) {
                EchoServer.messageController.appendMessage("Connection is null or closed. Reconnecting...");
                connectToDB();
            }
        } catch (SQLException e) {
            EchoServer.messageController.appendMessage("Error while checking connection state:");
            e.printStackTrace();
            return null;
        }
        return conn;
    }

    // Helper for checking if record exists
    private boolean recordExists(String query, Object... params) {
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean orderExists(int orderNumber) {
        return recordExists("SELECT 1 FROM orders WHERE order_number = ?", orderNumber);
    }

    public boolean checkParkingSpaceIfExists(int parkingSpaceToCheck) {
        return recordExists("SELECT 1 FROM parking_space WHERE parking_number = ? LIMIT 1", parkingSpaceToCheck);
    }

    public boolean isDateTakenForParkingSpace(int parkingSpace, Date date, int Order) throws Exception {
        return recordExists("SELECT 1 FROM orders WHERE parking_space = ? AND order_date = ? AND order_number != ? LIMIT 1",
                parkingSpace, date, Order);
    }

    public boolean checkEquals(int parkingSpace, Date date, int Order) {
        return recordExists("SELECT 1 FROM orders WHERE parking_space = ? AND order_date = ? AND order_number = ? LIMIT 1",
                parkingSpace, date, Order);
    }

    public String printOrderById(int orderNumber) {
        String query = "SELECT * FROM orders WHERE order_number = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, orderNumber);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("parking_space") + " " +
                        rs.getInt("order_number") + " " +
                        rs.getDate("order_date") + " " +
                        rs.getInt("confirmation_code") + " " +
                        rs.getInt("subscriber_id") + " " +
                        rs.getDate("date_of_placing_an_order") + " ";
            } else {
                return "No order found with number: " + orderNumber;
            }
        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        }
    }

    public boolean isDateValid(int orderNumber, Date requestedDate) throws Exception {
        String query = "SELECT date_of_placing_an_order FROM orders WHERE order_number = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, orderNumber);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Date placingDate = rs.getDate("date_of_placing_an_order");
                long placingTime = placingDate.getTime();
                long minAllowed = placingTime + 1L * 24 * 60 * 60 * 1000L;
                long maxAllowed = placingTime + 7L * 24 * 60 * 60 * 1000L;
                long requestedTime = requestedDate.getTime();
                return requestedTime >= minAllowed && requestedTime <= maxAllowed;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new Exception("Error checking date validity: " + e.getMessage());
        }
        return false;
    }

    public int updateParkingSpace(int orderNumber, int newSpace) throws Exception {
        if (!checkParkingSpaceIfExists(newSpace)) {
            EchoServer.messageController.appendMessage("Parking space does not exist.");
            return -1;
        }

        String selectQuery = "SELECT parking_space FROM orders WHERE order_number = ?";
        try (PreparedStatement stmt = conn.prepareStatement(selectQuery)) {
            stmt.setInt(1, orderNumber);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int currentSpace = rs.getInt("parking_space");
                if (currentSpace == newSpace) {
                    EchoServer.messageController.appendMessage("same parking space\n trying to change from: " + currentSpace + " to " + newSpace);
                    return 0;
                }
            } else {
                EchoServer.messageController.appendMessage("Order not found.");
                return -1;
            }
        }

        String updateQuery = "UPDATE orders SET parking_space = ? WHERE order_number = ?";
        try (PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
            stmt.setInt(1, newSpace);
            stmt.setInt(2, orderNumber);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                EchoServer.messageController.appendMessage("Parking space updated successfully.");
                return 1;
            } else {
                EchoServer.messageController.appendMessage("Failed to update parking space.");
                return -1;
            }
        } catch (SQLException e) {
            EchoServer.messageController.appendMessage("Error updating parking space: " + e.getMessage());
            return -1;
        }
    }

    public int updateOrderDate(int orderNumber, Date newDate) {
        if (!orderExists(orderNumber)) {
            EchoServer.messageController.appendMessage("Order does not exist");
            return -1;
        }

        String selectQuery = "SELECT order_date FROM orders WHERE order_number = ?";
        try (PreparedStatement stmt = conn.prepareStatement(selectQuery)) {
            stmt.setInt(1, orderNumber);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Date currentDate = rs.getDate("order_date");
                if (currentDate.equals(newDate)) {
                    EchoServer.messageController.appendMessage("same date nothing changed in date field");
                    return 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String updateQuery = "UPDATE orders SET order_date = ? WHERE order_number = ?";
        try (PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
            stmt.setDate(1, newDate);
            stmt.setInt(2, orderNumber);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                EchoServer.messageController.appendMessage("Order date updated successfully.");
                return 1;
            } else {
                EchoServer.messageController.appendMessage("Failed to update order date.");
                return -1;
            }
        } catch (SQLException e) {
            EchoServer.messageController.appendMessage("Error updating order date: " + e.getMessage());
            return -1;
        }
    }
}
