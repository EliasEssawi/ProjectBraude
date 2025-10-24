package guiUsher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import client.ClientUI;
import gui.SceneNavigator;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

/**
 * Controller class for the Usher main interface.
 * Handles user interactions such as viewing subscribers, 
 * adding a new subscriber, and showing active parking spots.
 * Also receives and displays messages from the server.
 * 
 * @author Bahaa
 */
public class UsherFrameController {

    /** Tracks item index if needed for future logic */
    private static int itemIndex = 5;

    /** Logout button at the top bar */
    @FXML private Button btnLogout;

    /** Logout button at the top bar */
    @FXML private Button SubscriberHistory;

    /** Logout button at the top bar */
    @FXML private Button AddTagToSubscriber;

    /** Button to view the list of subscribers */
    @FXML private Button btnSeeSub;

    /** Button to view currently active parking spots */
    @FXML private Button btnSeeActiv;

    /** Button to open the add-subscriber form */
    @FXML private Button btnAddSub;

    /** Label to show error messages */
    @FXML private Label lblError;

    /** Output area to display reports from server responses */
    @FXML private TextArea txtReportOutput;

    /**
     * Constructor that sets a reference to this controller in the ClientUI
     */
    public UsherFrameController() {
        ClientUI.usherFrameController = this;
    }

    /**
     * Handles logout action by navigating back to the Sign-In screen.
     * We also disconnect the client from server(connection as Usher)
     * @param event The action event triggered by logout button
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
     * Displays the content sent from the server based on the command received.
     * 
     * @param message A string message received from the server
     */
    public void display(String message) {
        ArrayList<String> response = new ArrayList<>(Arrays.asList(message.split(" ")));
        String func = response.get(0).trim();
        
        try {
            if (func.equals("SUBSCRIBERS_VIEW")) {
                // Display list of subscribers
                StringBuilder subView = new StringBuilder();
                String m = String.join(" ", response.subList(2, response.size())).trim();
                if(m.equals("There is no active parking spots found!"))
                {
                    txtReportOutput.setText(m);
                }
                else
                {
                	for (int i = 1; i < response.size(); i++) {
                		subView.append(response.get(i)).append(" ");
                	}
                	txtReportOutput.setText(subView.toString().trim());
                }

            } else if (func.equals("SHOW_ACTIVE_PARKINGSPOT")) {
                // Display list of active parking spots
                StringBuilder activePS = new StringBuilder();
                String m = String.join(" ", response.subList(2, response.size())).trim();
                if(m.equals("There is no active parking spots found!"))
                {
                    txtReportOutput.setText(m);
                }
                else {
	                for (int i = 1; i < response.size(); i++) {
	                    activePS.append(response.get(i)).append(" ");
	                }
	                txtReportOutput.setText(activePS.toString().trim());
               }

            } else if (func.equals("ADD_SUB_SUCCESSFULLY")) {
                // Show success message in AddSubController
                if (ClientUI.addSubController != null) {
                    ClientUI.addSubController.setMessage("Subscriber added successfully!\nSubscriber ID: " + response.get(1).trim(), false);
                }

            } else if (func.equals("ADD_SUB_FAILED_EXISTS")) {
                // Show error if subscriber already exists
                if (ClientUI.addSubController != null) {
                    ClientUI.addSubController.setMessage("Subscriber already exists: " + response.get(1).trim(), true);
                }

            } else if (func.equals("ADD_SUB_FAILED_INSERT")) {
                // Show error if subscriber insertion failed
                if (ClientUI.addSubController != null) {
                    ClientUI.addSubController.setMessage("Failed to insert: " + response.get(1).trim(), true);
                }
            }
            else if (func.equals("ERROR_SUBSCRIBER_ALREADY_HAS_TAG")) {
                txtReportOutput.setText("Error: This subscriber already has a Tag Reader.");
               

            }
            else if(func.equals("ShowHistory"))
            {
            	response.remove(0);
            	StringBuilder output = new StringBuilder();
            	for (String part : response) {
            	    output.append(part).append(" ");
            	}
            	txtReportOutput.setText(output.toString().trim());
            }
            else if (func.equals("ERROR_NO_SUCH_SUBSCRIBER")) {
                txtReportOutput.setText(" Error: Subscriber does not exist.");

            } else if (func.equals("ERROR_INSERT_TAGREADER") || func.equals("ERROR_SQL")) {
                txtReportOutput.setText("Error: Failed to insert Tag Reader. Please try again later.");

            } else if (func.equals("ADD_TAG_SUCCESS")) {
                if (response.size() >= 3) {
                    String subscriberId = response.get(1);
                    String tagId = response.get(2);
                    txtReportOutput.setText("Tag Reader successfully added!\nSubscriber ID: " + subscriberId + "\nTagReader ID: " + tagId);
                } else {
                    txtReportOutput.setText("Tag Reader added (missing details)");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles button click for viewing all subscribers.
     * Sends a message to the server to retrieve subscriber list.
     * 
     * @param event The action event triggered by the button
     */
    @FXML
    public void onSeeSubscribers(ActionEvent event) throws Exception {
        lblError.setText("Fetching all subscribers...");
        try {
            ArrayList<String> msg = new ArrayList<>();
            msg.add("GET_ALL_SUBSCRIBERS");
            ClientUI.chat.acceptArray(msg);
        } catch (Exception e) {
            lblError.setText("Error: " + e.getMessage());
        }
    }
    

    /**
     * Handles the click for adding a new subscriber.
     * Hides the current window and opens the Add Subscriber form.
     * 
     * @param event The action event triggered by the button
     */
    @FXML
    public void onAddNewSubscriber(ActionEvent event) throws Exception {
        Stage currentStage = (Stage) btnAddSub.getScene().getWindow();
        currentStage.hide();
        ClientUI.signInController.loadPage("/guiUsher/AddSub.fxml", "/guiUsher/AddSub.css", "Add Subscriber", "AddSubController");
        ClientUI.addSubController.setPreviousStage(currentStage);
    }
    
    @FXML
    public void onSubscriberHistory(ActionEvent event) throws Exception {
    	  TextInputDialog dialog = new TextInputDialog();
          dialog.setTitle("Subscriber History");
          dialog.setHeaderText("Show Subscriber History");
          dialog.setContentText("Please enter Subscriber ID:");

          dialog.showAndWait().ifPresent(subscriberId -> {
              if (subscriberId == null || subscriberId.trim().isEmpty()) {
                  lblError.setText("You must enter a Subscriber ID.");
                  return;
              }

              ArrayList<String> msg = new ArrayList<>();
              msg.add("SHOW_SUBSCRIBER_HISTORY");
              msg.add(subscriberId);

              try {
                  ClientUI.chat.acceptArray(msg);  // שליחה לשרת
                  lblError.setText("Show history for subscriber: " + subscriberId);
              } catch (Exception e) {
                  lblError.setText("Failed to send request for show history: " + e.getMessage());
              }
          });
    }
    
    @FXML
    public void onAddTagToSubscriber(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Tag Reader");
        dialog.setHeaderText("Add Tag Reader to Subscriber");
        dialog.setContentText("Please enter Subscriber ID:");

        dialog.showAndWait().ifPresent(subscriberId -> {
            if (subscriberId == null || subscriberId.trim().isEmpty()) {
                lblError.setText("You must enter a Subscriber ID.");
                return;
            }

            ArrayList<String> msg = new ArrayList<>();
            msg.add("ADD_TAG_READER");
            msg.add(subscriberId);

            try {
                ClientUI.chat.acceptArray(msg);  // שליחה לשרת
                lblError.setText("Tag Reader request sent for subscriber: " + subscriberId);
            } catch (Exception e) {
                lblError.setText("Failed to send add tag request: " + e.getMessage());
            }
        });
    }


    /**
     * Handles the click for viewing currently active parking spots.
     * Sends a request to the server to get the data.
     * 
     * @param event The action event triggered by the button
     */
    @FXML
    public void onSeeActiveParkingSpots(ActionEvent event) throws Exception {
        lblError.setText("Fetching active parking lots...");
        try {
            ArrayList<String> msg = new ArrayList<>();
            msg.add("GET_ACTIVE_PARKINGSPOT");
            ClientUI.chat.acceptArray(msg);
        } catch (Exception e) {
            lblError.setText("Error: " + e.getMessage());
        }
    }

    /**
     * Closes the current window. This method is not used in the current logic
     * but left for possible future use.
     */
    private void closeCurrentWindow() {
        // Placeholder method if you want to close this window later
    }
}
