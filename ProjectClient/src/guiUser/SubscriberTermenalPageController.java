package guiUser;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.time.Duration;

import client.ChatClient;
import client.ClientController;
import client.ClientUI;
import common.ChatIF;
import gui.NotificationController;
import gui.SceneNavigator;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import logic.Order;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.control.Label;

/**
 * Controller class for managing the subscriber terminal page.
 * Handles actions like reservation checking, car retrieval, parking without reservation,
 * checking available spots, and displaying active parking status.
 * 
 * This class is connected to a smart parking system client interface.
 * 
 * @author Bahaa
 */
public class SubscriberTermenalPageController {

	private static int itemIndex = 3;

    /** Label to show general error messages. */
    @FXML private Label lblError;

    /** Label showing how many spots are available. */
    @FXML private Label lblAvailableSpots;

    /** Logout button to disconnect and return to login screen. */
    @FXML private Button btnLogout;

    /** VBox showing available spots section. */
    @FXML private VBox vboxAvailableSpots;

    /** VBox containing the user's parking history. */
    @FXML private VBox historySection;

    /** Button to trigger showing history. */
    @FXML private Button btnShowHistory;

    /** Text field for entering reservation code. */
    @FXML private TextField txtReservationCode;

    /** Text field for entering car retrieve code. */
    @FXML private TextField txtRetrieveCode;

    /** VBox for showing reservation code input section. */
    @FXML private VBox vboxReservationCode;

    /** VBox for car retrieval section. */
    @FXML private VBox vboxRetrieveCode;

    /** VBox for current parking status section. */
    @FXML private VBox parkingStatusSection;

    /** Parent VBox containing all major screen sections. */
    @FXML private VBox Sections;

    /** Button to check reservation code. */
    @FXML private Button btnCheckReservationCode;

    /** Button to retrieve parked car. */
    @FXML private Button btnRetrieveCar;

    /** Button for forgotten parking code. */
    @FXML private Button btnForgotParkingCode;

    /** Label used for user feedback and notifications. */
    @FXML private Label lblNotification;

    /** VBox for handling parking without reservation. */
    @FXML private VBox vboxParkWithoutReservationSection;

    /** Label showing current parking code. */
    @FXML private Label lblParkingCode;

    /** Text area to show history content. */
    @FXML private TextArea txtHistoryArea;

    /** Combo box for selecting minutes to park. */
    @FXML private ComboBox<String> comboMinutesToPark;

    /** Combo box for selecting hours to park. */
    @FXML private ComboBox<String> comboHoursToPark;

    /** Label showing slot location. */
    @FXML private Label lblLocationStatus;

    /** Label showing when parking started. */
    @FXML private Label lblStartTimeStatus;

    /** Label showing maximum parking duration. */
    @FXML private Label lblMaxTimeStatus;

    /** Label showing time remaining or overdue. */
    @FXML private Label lblRemainingTimeStatus;

    /** Label showing status after car retrieval. */
    @FXML private Label lblRetriveCarStats;
    
    /**
     * Initializes combo box options for hour/minute selections with constraints for max parking time.
     */
    @FXML
    private void initialize() {
        // Populate hours (00-04)
    	
        for (int i = 0; i <= 4; i++) {
            comboHoursToPark.getItems().add(String.format("%02d", i));
        }

        // Populate minutes (00-55, every 5)
        for (int i = 0; i < 60; i += 5) {
            comboMinutesToPark.getItems().add(String.format("%02d", i));
        }

        // Set default selections
        comboHoursToPark.setValue("04");
        comboMinutesToPark.setValue("00");

        // Listener on hours: if 04 selected, force minutes to 00 and disable other options
        comboHoursToPark.setOnAction(e -> {
            String selectedHour = comboHoursToPark.getValue();
            if ("04".equals(selectedHour)) {
                comboMinutesToPark.setValue("00");
                comboMinutesToPark.setDisable(true); // disable to prevent user change
            } else {
                comboMinutesToPark.setDisable(false); // re-enable if hour < 04
            }
        });

        // Listener on minutes: if hour is 04, force it to 00 again (just in case)
        comboMinutesToPark.setOnAction(e -> {
            String selectedHour = comboHoursToPark.getValue();
            if ("04".equals(selectedHour)) {
                comboMinutesToPark.setValue("00"); // force correction
            }
        });
    }

    /**
     * Handles forgotten parking code by sending request and showing SMS notification.
     */
    @FXML
    private void forgotParkingCode()
    {
    	NotificationController.showNotification(lblNotification, 
 				"we will sent the parking code via SMS",
 				3, 
 				NotificationController.NotificationType.INFO);
    	ArrayList< String > str =new ArrayList<>();
        str.add("Forgot_Code");
        ClientUI.chat.acceptArray(str);
    }
    
    /**
     * Sends request to fetch user parking history.
     * @param event triggered by clicking "Show History"
     */
    @FXML
    private void onShowHistoryClicked(ActionEvent event) {
        boolean isVisible = historySection.isVisible();
        HideAllSectionExept(this.historySection);
        //btnShowHistory.setText(isVisible ? "Show History" : "Hide History");
        ArrayList<String> msg = new ArrayList<>();
        msg.add("GET_HISTORY");
        msg.add(ClientUI.loggedInUserID);
        msg.add("Termenal_Page");
        ClientUI.chat.acceptArray(msg);
        HideAllSectionExept(this.historySection);
    }
    
    /**
     * Displays retrieved parking history text.
     * @param historyText the formatted history string
     */
    public void displayHistory(String historyText) {
        txtHistoryArea.setText(historyText);
        HideAllSectionExept(this.historySection);
    }
    
    /**
     * Sends reservation code to server for validation.
     * @param event triggered when user submits code
     */
    @FXML
    private void checkReservationCode(ActionEvent event)
    {
    	
    	if(this.txtReservationCode.getText()==null)
    	{
    		NotificationController.showNotification(lblNotification, "Please enter your reservation code", 3, NotificationController.NotificationType.WARNING);
    	}
    	
    	else {
    	ArrayList< String > str =new ArrayList<>();
        str.add("Check_Reserve");
        str.add(this.txtReservationCode.getText());
        ClientUI.chat.acceptArray(str);
        HideAllSectionExept(vboxReservationCode); 
        }
    }
    
    /**
     * Requests the number of currently available parking spots.
     * @param event click event from "Check Available Spots"
     */
    @FXML
    private void checkAvilableSpotsBtn(ActionEvent event) throws Exception
    {
    	int availableSpots = 0;
    	
    	// Concatenate information into a single string command
    	ArrayList< String > str =new ArrayList<>();
        str.add("Check_Avilable_Spots_Termenal");
        ClientUI.chat.acceptArray(str);
        HideAllSectionExept(vboxAvailableSpots); 
    } 
    
    /**
     * Sends parking code to retrieve a parked car.
     * @param event click event from "Retrieve Car"
     */
    @FXML
    private void retrieveCar(ActionEvent event)
    {
    	//if the code is right
    	String parkingCode = this.txtRetrieveCode.getText();
    	if(parkingCode.equals(null) || parkingCode.equals(""))
    	{
    		NotificationController.showNotification(lblNotification, "Please enter your parking code", 3, NotificationController.NotificationType.WARNING);
    	}else
    	{
    		ArrayList< String > str =new ArrayList<>();
            str.add("Retrieve_Car_Termenal");
            str.add(parkingCode);
            ClientUI.chat.acceptArray(str);
    	}
    }
    
    /**
     * Shows the input section for entering reservation code manually.
     * @param event button event
     */
    @FXML
    private void onShowReservationInput(ActionEvent event) {
        HideAllSectionExept(vboxReservationCode);
    }

    /**
     * Requests the parking status of the user and shows retrieval input.
     * @param event button event
     */
    @FXML
    private void onShowRetrieveInput(ActionEvent event) {
    	ArrayList< String > str =new ArrayList<>();
        str.add("Get_My_Parking_Status_Termenal");
        ClientUI.chat.acceptArray(str);
    	HideAllSectionExept(this.vboxRetrieveCode);
    }

     /**
      * Opens section to park without reservation.
      * @param event click event
      */
    @FXML
    private void onParkNoReservationClicked(ActionEvent event) {
        HideAllSectionExept(this.vboxParkWithoutReservationSection);
    }
    
    /**
     * Sends request to park now without reservation, specifying desired parking time.
     * @param event click event
     */
    @FXML
    private void onParkNowClicked(ActionEvent event) {
        String hourStr = this.comboHoursToPark.getValue();
        String minuteStr = this.comboMinutesToPark.getValue();
        if(hourStr == null || minuteStr == null)
        {
        	hourStr = "4";
        	minuteStr = "0";
        }
       
        
        Integer totalMinutes = Integer.parseInt(hourStr)*60 + Integer.parseInt(minuteStr);
    	
        ArrayList< String > str =new ArrayList<>();
        str.add("Get_ParkingCode_Termenal");
        str.add(totalMinutes.toString());
        ClientUI.chat.acceptArray(str);
    }

    /**
     * Shows only the specified VBox section and hides all others.
     * @param Section the VBox to display
     */
    private void HideAllSectionExept(VBox Section)
    {
    	ObservableList<Node> AllSections = Sections.getChildren();
    	 
    	for(int i=0; i<AllSections.size(); i++)
    	{
    		if(Section != AllSections.get(i))
    		{
    			AllSections.get(i).setVisible(false);
    			AllSections.get(i).setManaged(false);
    		}else
    		{
    			AllSections.get(i).setVisible(true);
    			AllSections.get(i).setManaged(true);
    		}
    	}
    }
    
    /**
     * Handles user logout from the Terminal Page frame.
     *
     * This method disconnects the user from the server and navigates
     * back to the SignIn screen by loading the FXML again.
     * It uses a centralized SceneNavigator to avoid code duplication
     * across different controllers.
     * We also disconnect the client from server(connection as user)
     * @author Amit_Regev
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
     * Handles server messages and updates the UI accordingly.
     * Based on the message type, this method updates notification labels, displays reservation codes,
     * parking availability, active parking status, or history-related messages.
     * 
     * @param message Server response message string in format: STATUS [DETAILS...]
     */
    public void display(String message) {
        String[] str = message.split(" ");

        if (str[0].equals("Error")) {
            // Handle error messages
            switch (str[1]) {
                case "RETRIEVING_CAR_FIALED":
                    NotificationController.showNotification(lblNotification, str[1], 3, NotificationController.NotificationType.ERROR);
                    lblNotification.setText(str[1]);
                    lblRetriveCarStats.setText("An error occurred while retrieving the car. Please verify your code or use 'Forgot Code'. Contact usher if the issue persists.");
                    break;
                case "NO_CAR_TO_RETRIEVE":
                    NotificationController.showNotification(lblNotification, str[1], 3, NotificationController.NotificationType.ERROR);
                    lblNotification.setText(str[1]);
                    lblRetriveCarStats.setText("You don't have a parked car to retrieve.");
                    break;
                default:
                    NotificationController.showNotification(lblNotification, str[1], 3, NotificationController.NotificationType.ERROR);
                    lblNotification.setText(str[1]);
                    lblParkingCode.setText("There are no parking spots available at this time. Try adjusting your parking time.");
            }
        } else {
            // Extract the remaining message content if any
            String restMessage = (str.length > 1) ? String.join(" ", Arrays.copyOfRange(str, 1, str.length)) : "";

            // Handle different success/warning/info messages
            switch (str[0]) {
                case "Success":
                    NotificationController.showNotification(lblNotification, restMessage, 3, NotificationController.NotificationType.SUCCESS);
                    lblParkingCode.setText("Your reserved parking has started successfully.");
                    break;
                case "Failed":
                    NotificationController.showNotification(lblNotification, restMessage, 3, NotificationController.NotificationType.ERROR);
                    lblParkingCode.setText("Failed!");
                    break;
                case "PARKING_RESERVATION_ALREADY_USED":
                    NotificationController.showNotification(lblNotification, "Reservation already used", 3, NotificationController.NotificationType.WARNING);
                    lblParkingCode.setText("You have already used this reservation.");
                    break;
                case "GET_PARKING_CODE_SUCCESS":
                    NotificationController.showNotification(lblNotification, "Parking Code (SMS): " + restMessage, 3, NotificationController.NotificationType.SUCCESS);
                    lblParkingCode.setText("Your parking code is " + str[1]);
                    break;
                case "GET_PARKING_CODE_WARNING":
                    NotificationController.showNotification(lblNotification, str[1].replace("/", " "), 3, NotificationController.NotificationType.WARNING);
                    lblParkingCode.setText(str[1].replace("/", " "));
                    break;
                case "CHECK_SPOTS_SUCCESS":
                    NotificationController.showNotification(lblNotification, "Checking available spots succeeded", 3, NotificationController.NotificationType.SUCCESS);
                    lblAvailableSpots.setText("There are " + str[1] + " Available Spots");
                    break;
                case "GET_PARKING_STATUS_SUCCESS":
                    NotificationController.showNotification(lblNotification, "Checking status succeeded", 3, NotificationController.NotificationType.SUCCESS);

                    // Parse date and time of parking start
                    int maxTimeInMin = Integer.parseInt(str[4]);
                    String startTimeStr = str[2] + " " + str[3];
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    LocalDateTime startDateTime = LocalDateTime.parse(startTimeStr, formatter);
                    LocalDateTime now = LocalDateTime.now().minusMinutes(maxTimeInMin);
                    Duration duration = Duration.between(now, startDateTime);

                    long hours = duration.toHours();
                    long minutes = duration.toMinutes() % 60;

                    // Show remaining or overdue time
                    if (!duration.isNegative()) {
                        lblRemainingTimeStatus.setText("Remaining Time: " + hours + " hours " + minutes + " minutes");
                    } else {
                        lblRemainingTimeStatus.setText("You have been late for: " + Math.abs(hours) + " hours " + Math.abs(minutes) + " minutes");
                    }

                    // Set additional labels
                    lblLocationStatus.setText("Location: Slot " + str[1]);
                    lblStartTimeStatus.setText("Start Time: " + startTimeStr);
                    lblMaxTimeStatus.setText("Max Time: " + (maxTimeInMin / 60) + " hours " + (maxTimeInMin % 60) + " minutes");
                    break;
                case "GOT_EMPTY_STATUS":
                    // No active parking
                    lblLocationStatus.setText("It seems you do not have a current parked car.");
                    lblStartTimeStatus.setText("");
                    lblMaxTimeStatus.setText("");
                    lblRemainingTimeStatus.setText("");
                    break;
                case "RETRIEVING_CAR_SUCCESS":
                    NotificationController.showNotification(lblNotification, "Retrieving Car succeeded", 3, NotificationController.NotificationType.SUCCESS);
                    lblRetriveCarStats.setText("Your car is on its way.");
                    // Start a new thread that waits 5 seconds, then runs a function
                    new Thread(() -> {
                        try {
                            Thread.sleep(3000); // delay 3 seconds

                            // Now safely update UI
                            Platform.runLater(() -> {
                            	 lblRetriveCarStats.setText("");
                            });

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();  
                    break;
                case "PARKING_RESERVATION_SUCCESS":
                    NotificationController.showNotification(lblNotification, "Reservation parking succeeded", 3, NotificationController.NotificationType.SUCCESS);
                    lblParkingCode.setText("Your reserved parking has started successfully.");
                    break;
                case "PARKING_RESERVATION_FAILED":
                    NotificationController.showNotification(lblNotification, "Reservation failed: Not found", 3, NotificationController.NotificationType.ERROR);
                    lblParkingCode.setText("Reservation not found.");
                    break;
                case "PARKING_RESERVATION_FAILED_TIME_PARSE":
                    NotificationController.showNotification(lblNotification, "Reservation failed: Time format error", 3, NotificationController.NotificationType.ERROR);
                    lblParkingCode.setText("Reservation time could not be parsed.");
                    break;
                case "PARKING_RESERVATION_FAILED_UPDATE_SPOTID":
                    NotificationController.showNotification(lblNotification, "Reservation failed: Could not update spot", 3, NotificationController.NotificationType.ERROR);
                    lblParkingCode.setText("Could not mark the spot as occupied.");
                    break;
                case "PARKING_RESERVATION_FAILED_INSERT_HISTORY":
                    NotificationController.showNotification(lblNotification, "Reservation failed: Could not insert history", 3, NotificationController.NotificationType.ERROR);
                    lblParkingCode.setText("Could not insert parking history.");
                    break;
                default:
                    NotificationController.showNotification(lblNotification, message, 3, NotificationController.NotificationType.DEBUG);
            }
        }
    }
}
