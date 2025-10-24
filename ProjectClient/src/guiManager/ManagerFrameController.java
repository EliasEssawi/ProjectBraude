package guiManager;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;

import client.ClientUI;
import gui.SceneNavigator;
import javafx.event.ActionEvent;

/**
 * Controller class for the Manager frame in the GUI.
 * 
 * <p>Provides UI event handling and server communication for:
 * - Viewing parking reports
 * - Viewing subscription reports
 * - Fetching active parking spots
 * - Fetching all subscribers
 * - Displaying textual and visual reports
 * 
 * This class uses {@link ClientUI} to send requests to the server,
 * and updates JavaFX components accordingly.
 * 
 * @author Group 17
 */
public class ManagerFrameController {

    // FXML UI elements
    @FXML private Button btnLogout;
    @FXML private Button btnSend;
    @FXML private Button btnSubscriptionReports;
    @FXML private Button btnActiveParking;
    @FXML private Label lblError;
    @FXML private DatePicker datePickerParking;
    @FXML private DatePicker datePickerSubscription;
    @FXML private TextField txtSubscriptionInput;
    @FXML private javafx.scene.control.TextArea txtReportOutput;

    /**
     * Handles user logout from the Manager frame.
     *
     * This method disconnects the user from the server and navigates
     * back to the SignIn screen by loading the FXML again.
     * It uses a centralized SceneNavigator to avoid code duplication
     * across different controllers.
     * We also disconnect the client from server(connection as Manager)
     * @param event the ActionEvent triggered by clicking the "Logout" button
     */
    @FXML
    public void onLogoutClicked(ActionEvent event) {
    	  ArrayList<String> msg = new ArrayList<>();
          msg.add("LOGOUT");

          // Run in background thread to avoid freezing UI
          new Thread(() -> {
              try {
                  if (ClientUI.chat != null) {
                      ClientUI.chat.acceptArray(msg);
                  } else {
                      System.out.println("ClientUI.chat is null.");
                  }
              } catch (Exception e) {
                  e.printStackTrace(); // just in case
              }
          }).start();

          // Continue immediately with scene change
          Stage stage = (Stage) btnLogout.getScene().getWindow();
          SceneNavigator.logoutAndReturnToLogin(stage);
       
      
    }

    /**
     * Handles the action when the "View parking reports" button is clicked.
     * 
     * <p>This method checks whether the user has selected a date from the DatePicker.
     * If not, it displays an error. Otherwise, it sends a "PARKING_REPORT" request to the server.
     *
     * @param event the ActionEvent triggered by the button click
     */
    @FXML
    public void ViewParkingReports(ActionEvent event) {
        if (datePickerParking.getValue() == null) {
            lblError.setText("Please select a date for the parking report.");
            return;
        }

        String parkingInput = datePickerParking.getValue().toString();
        lblError.setText("Fetching parking reports for: " + parkingInput);

        ArrayList<String> msg = new ArrayList<>();
        msg.add("PARKING_REPORT");
        msg.add(parkingInput);

        ClientUI.chat.acceptArray(msg);
    }

    /**
     * Handles the action when the "View subscription reports" button is clicked.
     * 
     * <p>Validates that the subscription code and date are provided.
     * Sends "SUBSCRIPTION_REPORT" command with input data to the server.
     *
     * @param event the ActionEvent triggered by the button click
     */
    @FXML
    public void onSubscriptionReportsClicked(ActionEvent event) {
        String subscriptionInput = txtSubscriptionInput.getText();

        if (subscriptionInput == null || subscriptionInput.trim().isEmpty()) {
            lblError.setText("Please enter subscription code.");
            return;
        }

        if (datePickerSubscription.getValue() == null) {
            lblError.setText("Please select a date for the subscription report.");
            return;
        }

        String dateInput = datePickerSubscription.getValue().toString();
        lblError.setText("Fetching subscription reports for: " + subscriptionInput + " on " + dateInput);

        ArrayList<String> msg = new ArrayList<>();
        msg.add("SUBSCRIPTION_REPORT");
        msg.add(subscriptionInput);
        msg.add(dateInput);

        ClientUI.chat.acceptArray(msg);
    }

    /**
     * Sends a request to the server to fetch active parking spots.
     *
     * <p>This is triggered by clicking the "Active Parking" button in the manager UI.
     * 
     * @param event the ActionEvent triggered by the button click
     * @author Amit_Regev
     */
    @FXML
    public void onActiveParkingClicked(ActionEvent event) {
        lblError.setText("Fetching active parking lots...");

        ArrayList<String> msg = new ArrayList<>();
        msg.add("GET_ACTIVE_PARKINGSPOT");

        ClientUI.chat.acceptArray(msg);
    }

    /**
     * Displays a textual report in the report output text area.
     *
     * @param reportText The string to display in the report output area.
     * @author Amit_Regev
     */
    public void displayMsg(String reportText) {
        txtReportOutput.setText(reportText);
    }

    /**
     * Sends a request to the server to fetch all subscribers.
     * 
     * <p>Triggered by a dedicated "Get Parking Subscribers" button in the manager UI.
     *
     * @param event the ActionEvent triggered by the button click
     */
    @FXML
    public void onGetParkingSubcsribersClicked(ActionEvent event) {
        lblError.setText("Fetching all subscribers...");

        ArrayList<String> msg = new ArrayList<>();
        msg.add("GET_ALL_SUBSCRIBERS");

        ClientUI.chat.acceptArray(msg);
    }

    /**
     * Displays an image (report) in a popup window.
     * 
     * <p>This method is called when a visual report (e.g., chart or image) is received
     * from the server. It opens a new window and displays the image.
     *
     * @param imageBytes The image data in byte array format.
     */
    public void displayReportImage(byte[] imageBytes) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
            Image fxImage = new Image(bis);

            ImageView imageView = new ImageView(fxImage);
            imageView.setFitWidth(500);
            imageView.setPreserveRatio(true);

            VBox box = new VBox(imageView);
            box.setPadding(new Insets(10));

            Stage stage = new Stage();
            stage.setTitle("Visual Report Image");
            stage.setScene(new Scene(box));
            stage.show();
        } catch (Exception e) {
            lblError.setText("Failed to display visual report image.");
        }
    }
}
