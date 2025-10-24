package guiUsher;

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
import logic.Subscriber;
import javafx.scene.control.Label;

/**
 * Controller class for the 'Add New Subscriber' page in the Usher module.
 * Handles user interactions such as saving subscriber information and closing the window.
 * Communicates with the server via ClientUI.chat to submit subscriber data.
 *
 * @author Bahaa
 */
public class AddSubController {

    private Subscriber s;

    /** Label for the page title */
    @FXML
    private Label lblAddANewSubscriber;

    /** Label for the username field */
    @FXML
    private Label lblUserName;

    /** Label for the phone number field */
    @FXML
    private Label lblPhoneNumber;

    /** Label for the email field */
    @FXML
    private Label lblEmail;

    /** Input field for subscriber username */
    @FXML
    private TextField txtUserName;

    /** Input field for subscriber phone number */
    @FXML
    private TextField txtPhoneNumber;

    /** Input field for subscriber email */
    @FXML
    private TextField txtEmail;

    /** Button to close the window */
    @FXML
    private Button btnclose = null;

    /** Static reference to the error label for shared access */
    private static Label staticErrorLabel;

    /** Button to save subscriber information */
    @FXML
    private Button btnsave;

    /** Label for displaying error or success messages */
    @FXML
    private Label ErrorMessage;

    /** Label for displaying internal errors */
    @FXML
    private Label lblError;

    /** Reference to the previous stage to return to when this window closes */
    private Stage previousStage;

    /**
     * Initializes the controller, setting the static reference to the error label.
     */
    @FXML
    private void initialize() {
        staticErrorLabel = ErrorMessage;
    }

    /**
     * Sets the previous stage to be shown again after this window is closed.
     *
     * @param stage The previous stage
     */
    public void setPreviousStage(Stage stage) {
        this.previousStage = stage;
    }

    /**
     * Closes the current window and returns to the previous stage.
     *
     * @param event ActionEvent triggered by the close button
     * @throws Exception if an error occurs while closing
     */
    @FXML
    public void onClose(ActionEvent event) throws Exception {
        try {
            closeCurrentWindow();
            if (previousStage != null) {
                previousStage.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Displays a message in the error label with optional color for error/success.
     *
     * @param message  The message to display
     * @param isError  True if it's an error message, false for success
     */
    public void setMessage(String message, boolean isError) {
        ErrorMessage.setText(message);
        if (isError) {
            ErrorMessage.setStyle("-fx-text-fill: red;");
        } else {
            ErrorMessage.setStyle("-fx-text-fill: green;");
        }
    }

    /**
     * Sends subscriber data to the server to add a new subscriber.
     *
     * @param event ActionEvent triggered by the save button
     * @throws Exception if message submission fails
     */
    public void onSave(ActionEvent event) throws Exception {
        try {
            ArrayList<String> msg = new ArrayList<>();
            msg.add("ADD_SUB");
            msg.add(this.txtUserName.getText());
            msg.add(this.txtPhoneNumber.getText());
            msg.add(this.txtEmail.getText());

            ClientUI.chat.acceptArray(msg);
        } catch (Exception e) {
            lblError.setText("Error: " + e.getMessage());
        }
    }

    /**
     * Closes the current window.
     */
    private void closeCurrentWindow() {
        Stage currentStage = (Stage) btnclose.getScene().getWindow();
        currentStage.close();
    }
}
