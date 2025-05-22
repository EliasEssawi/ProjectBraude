package gui;

import java.io.IOException;
import java.util.ArrayList;

import client.ChatClient;
import client.ClientController;
import client.ClientUI;
import common.ChatIF;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import logic.Order;
import javafx.scene.control.Label;

public class WorkerFrameController {

    // Reference to OrderFormController for further use
    private OrderFormController sfc;

    // Index for some item logic, not used in this class
    private static int itemIndex = 3;

    // Exit button component
    @FXML
    private Button btnExit = null;

    // Send button component
    @FXML
    private Button btnSend = null;

    // Text field to input order ID
    @FXML
    private TextField idtxt;

    // Label for showing error messages
    @FXML
    private Label lblError;

    // Helper method to get the text entered in the ID text field
    private String getID() {
        return idtxt.getText();
    }

    // This method is triggered when the Send button is clicked
    public void Send(ActionEvent event) throws Exception {
        String id;
        FXMLLoader loader = new FXMLLoader();

        // Get the ID from the text field
        id = getID();
        ArrayList<String> ArrSend=new ArrayList<>();
        ArrSend.add(id);
        // Check if ID is empty
        if (id.trim().isEmpty()) {
            System.out.println("You must enter an id number");
        } else {
            try {
                // Send the ID to the server
            	
                ClientUI.chat.acceptArray(ArrSend);
            } catch (Exception e) {
                // Display error if sending fails
                lblError.setText("Error: " + e.getMessage());
            }

            // Check if a valid order was returned
            if (ChatClient.emptyOrder.getOrderNumber() == 0) {
                lblError.setText("Order Number Not Found");
            } else {
                // Hide current window
                ((Node) event.getSource()).getScene().getWindow().hide();

                // Load the OrderForm.fxml GUI
                Stage primaryStage = new Stage();
                Pane root = loader.load(getClass().getResource("/gui/OrderForm.fxml").openStream());

                // Get controller of loaded FXML and pass order data
                OrderFormController studentFormController = loader.getController();
                studentFormController.loadStudent(ChatClient.emptyOrder);

                // Create and set up the new scene
                Scene scene = new Scene(root);
                scene.getStylesheets().add(getClass().getResource("/gui/OrderForm.css").toExternalForm());
                primaryStage.setTitle("Student Management Tool");
                primaryStage.setScene(scene);
                primaryStage.show();
            }
        }
    }

    /*
    // (Commented out) This method initializes and displays the WorkerFrame window
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/gui/WorkerFrame.fxml"));
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/gui/WorkerFrame.css").toExternalForm());
        primaryStage.setTitle("Orders Management Tool");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    */

    // Method to handle exit button action
    public void getExitBtn(ActionEvent event) throws Exception {
        System.out.println("exit Academic Tool");

        // Disconnect from server and exit application
        ClientUI.chat.disconnect();
        Thread.sleep(500);
        System.exit(0);
    }

    // Passes order object to another controller for loading
    public void loadorde(Order o1) {
        this.sfc.loadStudent(o1);
    }

    // Prints a received message to the console (stub method)
    public void display(String message) {
        System.out.println("message");
    }
}
