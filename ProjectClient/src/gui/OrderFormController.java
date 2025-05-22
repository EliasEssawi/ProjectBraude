package gui;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import client.ChatClient;
import client.ClientUI;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import logic.Order;

public class OrderFormController {

    // Current order object to display/edit
    private Order o;

    // Labels for displaying order data
    @FXML private Label lblParkingSpace;
    @FXML private Label lblOrderNumber;
    @FXML private Label lblOrderDate;
    @FXML private Label lblConfirmationCode;
    @FXML private Label lblSubscriberID;
    @FXML private Label lblDateOfPlacingAnOrder;

    // Text fields and labels for showing values
    @FXML private TextField txtParkingSpace;
    @FXML private Label txtOrderNumber;
    @FXML private TextField txtOrderDate;
    @FXML private Label txtConfirmationCode;
    @FXML private Label txtSubscriberID;
    @FXML private Label txtDateOfPlacingAnOrder;

    // Close button
    @FXML private Button btnclose = null;

    // Static reference to error label
    private static Label staticErrorLabel;

    // Label for displaying error messages
    @FXML private Label ErrorMessage;

    // Initialize method called automatically after FXML load
    @FXML
    private void initialize() {
        staticErrorLabel = ErrorMessage;
    }

    // Loads an Order object into the form
    public void loadStudent(Order o1) {
        ClientUI.currentOrderFormController = this; // Set current controller reference
        this.o = o1;

        // Populate fields with order data
        this.txtParkingSpace.setText(o.getParkingSpace() + "");
        this.txtOrderNumber.setText(o.getOrderNumber() + "");
        this.txtOrderDate.setText(o.getOrderDate().toString());
        this.txtConfirmationCode.setText(o.getConfirmationCode() + "");
        this.txtSubscriberID.setText(o.getSubscriberId() + "");
        this.txtDateOfPlacingAnOrder.setText(o.getDateOfPlacingAnOrder().toString());
    }

    // Handles close button: returns to WorkerFrame
    public void onClose(ActionEvent event) throws Exception {
        try {
            // Clear the static emptyOrder
            ChatClient.emptyOrder = new Order();

            // Hide current window
            ((Node) event.getSource()).getScene().getWindow().hide();

            // Load the WorkerFrame.fxml GUI
            Parent newRoot = FXMLLoader.load(getClass().getResource("WorkerFrame.fxml"));

            // Set new scene to the same stage
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(newRoot));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Static method to show error message from anywhere
    public static void showErrorMessage(String message) {
        if (ClientUI.currentOrderFormController != null) {
            ClientUI.currentOrderFormController.setErrorMessage(message);
        }
    }

    // Sets the error message label
    public void setErrorMessage(String message) {
        ErrorMessage.setText(message);
    }

    // Handles save button: sends edited order data to server
    public void onSave(ActionEvent event) throws Exception {
       ArrayList< String > str =new ArrayList<>();
        // Concatenate updated order information into a single string command
        str.add(txtParkingSpace.getText());
        str.add((this.o.getOrderNumber()+""));
        str.add(this.txtOrderDate.getText());  
      // Send the edit command to the server
        ClientUI.chat.acceptArray(str);
    }

}
