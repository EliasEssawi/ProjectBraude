package guiUser;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

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
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import logic.Order;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.animation.PauseTransition;
import java.time.Duration;

import javafx.scene.control.Label;

/**
 * Controller class for managing the subscriber's interface while accessing the system away from a terminal.
 * Handles UI interactions for reservation, extension, personal data update, history viewing, and logout.
 * 
 * This class communicates with the server to perform actions related to smart parking.
 * 
 * @author Group 17
 */
public class SubscriberAwayPageController {

	private String ID;

    /** Label to display general errors. */
    @FXML private Label lblError;

    /** Label to display the number of available parking spots. */
    @FXML private Label lblAvailableSpots;

    /** VBox section showing available spots. */
    @FXML private VBox vboxAvailableSpots;

    /** VBox section for displaying user history. */
    @FXML private VBox historySection;

    /** Button to trigger history view. */
    @FXML private Button btnShowHistory;

    /** Text field displaying generated reservation code. */
    @FXML private TextField txtReservationCode;

    /** Text field for entering a reservation retrieval code. */
    @FXML private TextField txtRetrieveCode;

    /** VBox for showing the reservation code after success. */
    @FXML private VBox vboxReservationCode;

    /** VBox for entering the code to retrieve reservation. */
    @FXML private VBox vboxRetrieveCode;

    /** VBox section for reservation input. */
    @FXML private VBox vboxReservation;

    /** VBox section for extending parking time. */
    @FXML private VBox vboxExtendParking;

    /** ComboBox to choose minutes for parking. */
    @FXML private ComboBox<String> comboMinutesToPark;

    /** ComboBox to choose hours for parking. */
    @FXML private ComboBox<String> comboHoursToPark;

    /** Section showing current parking status. */
    @FXML private VBox parkingStatusSection;

    /** Logout button. */
    @FXML private Button btnLogout;

    /** Parent container for all screen sections. */
    @FXML private VBox Sections;

    /** Label used to display notifications for user feedback. */
    @FXML private Label lblNotification;

    /** ComboBox for selecting hour value when extending parking. */
    @FXML private ComboBox<String> comboHour;

    /** ComboBox for selecting minute value when extending parking. */
    @FXML private ComboBox<String> comboMinute;

    /** Date picker for reservation date. */
    @FXML private DatePicker datePickerReservation;

    /** ComboBox to select reservation hour. */
    @FXML private ComboBox<String> comboHourReserve;

    /** ComboBox to select reservation minute. */
    @FXML private ComboBox<String> comboMinuteReserve;

    /** Text field for entering phone number in personal data. */
    @FXML private TextField txtPhone;

    /** Text field for entering email address in personal data. */
    @FXML private TextField txtEmail;

    /** Text area for displaying subscriber's parking history. */
    @FXML private TextArea txtHistoryArea;

    /** Button to save changes to personal information. */
    @FXML private Button btnSaveChanges;

    /** VBox section for personal data update form. */
    @FXML private VBox vboxUpdatePersonal;

    /** Label to show subscriber ID in the personal data section. */
    @FXML private Label lblSubID;

    /** Label to show subscriber name in the personal data section. */
    @FXML private Label lblSubName;
    
    
    /**
     * Initializes combo boxes for time and duration selection with default values and constraints.
     */
    @FXML
    private void initialize() {
        // Populate hour ComboBox (00-23)
        for (int i = 0; i < 24; i++) {
        
            comboHour.getItems().add(String.format("%02d", i));
        }

        // Populate minute ComboBox (00, 15, 30, 45)
        comboMinute.getItems().addAll("00", "15", "30", "45");
        for (int i = 0; i < 24; i++) {
            
        	comboHourReserve.getItems().add(String.format("%02d", i));
        }
		for (int i = 0; i < 59; i++) {
		            
			comboMinuteReserve.getItems().add(String.format("%02d", i));
		        }
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
     * Handles click on "Show History" button.
     * Sends request to fetch subscriber history.
     */
    @FXML
    private void onShowHistoryClicked(ActionEvent event) {
        //boolean isVisible = historySection.isVisible();
        HideAllSectionExept(this.historySection);
        //btnShowHistory.setText(isVisible ? "Show History" : "Hide History");
        ArrayList<String> msg = new ArrayList<>();
        msg.add("GET_HISTORY");
        msg.add(ClientUI.loggedInUserID);
        msg.add("Away_Page");
        ClientUI.chat.acceptArray(msg);
        txtHistoryArea.setVisible(true);
    }
    
    /**
     * Sends request to fetch and display personal data for the subscriber.
     */
    @FXML
    private void onUpdatePersonalClicked(ActionEvent event) {
    	ArrayList< String > str =new ArrayList<>();
        str.add("Personal_Data");
        str.add(ClientUI.loggedInUserID);
        str.add("Away_Page");
        txtHistoryArea.setVisible(false);
        ClientUI.chat.acceptArray(str);
    }
    
    /**
     * Sends updated phone and email information to the server.
     */
    @FXML
    private void onSavePersonalChanges(ActionEvent event) {
        ArrayList<String> msg = new ArrayList<>();
        msg.add("UPDATE_PERSONAL_DATA");
        msg.add(ClientUI.loggedInUserID);
        msg.add("Away_Page");
        msg.add(txtPhone.getText().trim());
        msg.add(txtEmail.getText().trim());
        ClientUI.chat.acceptArray(msg);
    }
    
    /**
     * Displays the subscriber's history in the text area.
     * @param historyText The history to be shown
     */
	public void displayHistory(String historyText) {
	    txtHistoryArea.setText(historyText);
	    HideAllSectionExept(this.historySection);
	}
	
	/**
     * Displays fetched personal data (ID, name, phone, email) on screen.
     * @param message the formatted data string received from the server
     */
	public void displayPersonalData(String message) {
	    String[] parts = message.split(" ");
	    if (parts.length >= 6 && parts[1].equals("SHOW_PERSONAL_DATA")) {
	        lblSubID.setText(parts[2]);
	        lblSubName.setText(parts[3]); 
	        txtPhone.setText(parts[4]);
	        txtEmail.setText(parts[5]);
	
	        //vboxUpdatePersonal.setVisible(true);
	        //vboxUpdatePersonal.setManaged(true);
	        HideAllSectionExept(this.vboxUpdatePersonal);
	    } else {
	        System.out.println("Invalid personal data message received: " + message);
	    }
	}
	
	/**
     * Shows a notification result after attempting to save personal changes.
     * @param success true if update was successful, false otherwise
     */
	public void showUpdateResult(boolean success) {
	    if (success) {
	        NotificationController.showNotification(lblNotification, "Update successful!", 3, NotificationController.NotificationType.SUCCESS);
	    } else {
	        NotificationController.showNotification(lblNotification, "Update failed. Please try again.", 3, NotificationController.NotificationType.ERROR);
	    }
	}
    
	/**
     * Sends request to check available parking spots.
     */
    @FXML
    public void checkAvilableSpotsBtn(ActionEvent event) throws Exception
    {
    	int availableSpots = 0;
    	
    	// Concatenate information into a single string command
    	ArrayList< String > str =new ArrayList<>();
        str.add("Check_Avilable_Spots");
        ClientUI.chat.acceptArray(str);
    }
    
    /**
     * Displays the reservation form to the user.
     */
    @FXML
    private void showReserveBtn(ActionEvent event) {
        HideAllSectionExept(this.vboxReservation);
        if (comboHourReserve.getValue() == null)
        	comboHourReserve.setValue("12"); // or any default

        if (comboMinuteReserve.getValue() == null)
        	comboMinuteReserve.setValue("00");
    }

    /**
     * Handles reservation submission, performs input validation and sends request.
     */
    @FXML
    private void onReserving(ActionEvent event) {
        String dateStr = this.datePickerReservation.getValue() != null ? this.datePickerReservation.getValue().toString() : null;
        String hourStr = this.comboHourReserve.getValue();
        String minuteStr = this.comboMinuteReserve.getValue();

        if (dateStr == null || hourStr == null || minuteStr == null) {
            NotificationController.showNotification(lblNotification, 
                "Please select date, hour and minute", 3, 
                NotificationController.NotificationType.WARNING);
            return;
        }
        String hourStrPark = this.comboHoursToPark.getValue();
        String minuteStrPark = this.comboMinutesToPark.getValue();
        if(hourStr == null || minuteStr == null)
        {
        	hourStr = "4";
        	minuteStr = "0";
        }
       
        
        Integer totalMinutes = Integer.parseInt(hourStrPark)*60 + Integer.parseInt(minuteStrPark);
    	

        LocalDateTime selectedDateTime = LocalDateTime.of(
            datePickerReservation.getValue(),
            LocalTime.of(Integer.parseInt(hourStr), Integer.parseInt(minuteStr))
        );

        LocalDateTime now = LocalDateTime.now();

        long minutesBetween = Duration.between(now, selectedDateTime).toMinutes();

       if (minutesBetween < 24 * 60 || minutesBetween > 7 * 24 * 60) {
            NotificationController.showNotification(lblNotification, 
                "Reservation must be at least 24 hours from now and within 7 days.",
                3, NotificationController.NotificationType.WARNING);
        } else {
            String Reserve = dateStr + " " + hourStr + " " + minuteStr;
            String Timetopark=Integer.toString(totalMinutes);

            ArrayList<String> str = new ArrayList<>();
            str.add("Reserve");
            
            str.add(Reserve);
            str.add(Timetopark);

            ClientUI.chat.acceptArray(str);
        }
    }
    
    /**
     * Sends request to check if there is an active parking to allow extension.
     * @author Amit_Regev
     */
    @FXML
    private void extendParkingTime(ActionEvent event) {
        ArrayList<String> msg = new ArrayList<>();
        msg.add("CHECK_ACTIVE_PARKING");
        msg.add(ClientUI.loggedInUserID);
        msg.add("Away_Page");

        ClientUI.chat.acceptArray(msg);
    }
    
    /**
     * Confirms and submits a parking extension request.
     * @param event the triggering event
     * @author Amit_Regev
     */
    @FXML
    private void onConfirmExtendParking(ActionEvent event) {
        String selectedHoursStr = comboHour.getValue();
        String selectedMinutesStr = comboMinute.getValue(); 

        if (selectedHoursStr == null || selectedMinutesStr == null){
            NotificationController.showNotification(lblNotification,"Please select both hours and minutes to extend",3,NotificationController.NotificationType.WARNING);
            return;
        }

        int hours = Integer.parseInt(selectedHoursStr);
        int minutes = Integer.parseInt(selectedMinutesStr);

        if (hours == 0 && minutes == 0) {
            NotificationController.showNotification( lblNotification,"Extension must be at least 15 minutes",3,NotificationController.NotificationType.WARNING );
            return;
        }

        int totalMinutes = hours * 60 + minutes;

        ArrayList<String> msg = new ArrayList<>();
        msg.add("EXTEND_PARKING");
        msg.add(ClientUI.loggedInUserID);
        msg.add(String.valueOf(totalMinutes));
        msg.add("Away_Page");

        ClientUI.chat.acceptArray(msg);
    }
    
    /**
     * Shows only the selected section and hides the rest.
     * @param Section the VBox to show
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
     * Loads the subscriber ID into the controller.
     * @param id the subscriber's ID
     */
    public void loadID(String id)
    {
    	this.ID=id;	
    }
    
    /**
     * Handles user logout from the Away Page frame.
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
     * Displays server response messages and updates the UI accordingly.
     * @param message the response message from the server
     */
    public void display(String message) {
        String[] str = message.split(" ");

        String status = str[0];

        switch (status) {

            case "Error":
                NotificationController.showNotification(lblNotification, str[1], 3, NotificationController.NotificationType.ERROR);
                lblNotification.setText(str[1]);
                break;

            case "CHECK_SPOTS_SUCCESS":
                NotificationController.showNotification(lblNotification, "Checking available spots succeeded", 3, NotificationController.NotificationType.SUCCESS);
                lblAvailableSpots.setText("There are " + str[1] + " Available Spots");
                HideAllSectionExept(this.vboxAvailableSpots);
                break;
            case "RESERVE_TIME_ERROR":
            	NotificationController.showNotification(lblNotification, "Error! Time is not Acceptable", 3, NotificationController.NotificationType.ERROR);
                lblNotification.setText("Error! Time is not Acceptable");
                break;
            case "CHECK_SPOTS_FAIL":
                NotificationController.showNotification(lblNotification, "Error during spot checking", 3, NotificationController.NotificationType.ERROR);
                lblNotification.setText("Error during spot checking");
                break;

            case "RESERVE_SUCCESS":
                String reservationCode = str[1];
                NotificationController.showNotification(lblNotification, "Reservation succeeded. Code: " + reservationCode, 3, NotificationController.NotificationType.SUCCESS);
                txtReservationCode.setText(reservationCode);
                HideAllSectionExept(this.vboxReservationCode);
                break;

            case "RESERVE_TIME_FAIL":
                NotificationController.showNotification(lblNotification, "Reservation failed: time slot unavailable", 3, NotificationController.NotificationType.WARNING);
                lblNotification.setText("Reservation failed: time slot unavailable");
                break;
            case "RESERVE_Exist":
                NotificationController.showNotification(lblNotification, "Reservation failed: there is reservation for this date", 3, NotificationController.NotificationType.WARNING);
                lblNotification.setText("Reservation failed:1 reservation for same day!");
                break;
            case "RESERVE_FAIL":
                NotificationController.showNotification(lblNotification, "Reservation failed, check if reservation exist for this date!", 3, NotificationController.NotificationType.ERROR);
                lblNotification.setText("Reservation failed, no 40% empty spots at least!");
                break;
            case "NO_ACTIVE_PARKING":
                NotificationController.showNotification(lblNotification, "Dear customer, you are not currently parked in our smart parking lot.", 3, NotificationController.NotificationType.WARNING);
                break;

            case "ACTIVE_PARKING":
                HideAllSectionExept(vboxExtendParking);
                break;
            case "EXTENSION_GRANTED:":
                NotificationController.showNotification(lblNotification, message.substring("EXTENSION_GRANTED:".length()).trim(), 3, NotificationController.NotificationType.SUCCESS);
                break;

            case "EXTENSION_DENIED:":
                NotificationController.showNotification(lblNotification, message.substring("EXTENSION_DENIED:".length()).trim(), 3, NotificationController.NotificationType.WARNING);
                break;

            default:
                NotificationController.showNotification(lblNotification, "Unknown response: " + message, 3, NotificationController.NotificationType.WARNING);
                lblNotification.setText("Unknown response received.");
        }
    }

}