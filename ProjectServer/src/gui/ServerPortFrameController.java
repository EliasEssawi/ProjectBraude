package gui;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import server.ServerUI;

/**
 * <p>
 * This controller manages the server port input window for starting the server.
 * It provides functionality to read the user-specified port and initialize the server accordingly,
 * or exit the application if requested.
 * </p>
 * 
 * <p>
 * UI components are mapped from the corresponding FXML file `ServerPort.fxml`, and user interactions
 * such as "Done" and "Exit" button clicks are handled here.
 * </p>
 * 
 * @author Bahaa
 */
public class ServerPortFrameController {

    private String temp = "";

    @FXML
    private Button btnExit = null;

    @FXML
    private Button btnDone = null;

    @FXML
    private Label lbllist;

    @FXML
    private TextField portxt;

    /**
     * Retrieves the text entered in the port input field.
     *
     * @return the port string entered by the user
     */
    private String getport() {
        return portxt.getText();
    }

    /**
     * Handles the Done button click event.
     * 
     * <p>
     * If a port number is entered, hides the current window and attempts to start
     * the server using {@link ServerUI#runServer(String)}.
     * If the field is empty, prints a message to the console.
     * </p>
     *
     * @param event the action event triggered by clicking the Done button
     * @throws Exception if server startup fails
     */
    public void Done(ActionEvent event) throws Exception {
        String p = getport();

        if (p.trim().isEmpty()) {
            System.out.println("You must enter a port number");
        } else {
            ((Node) event.getSource()).getScene().getWindow().hide(); // Hides the current window
            ServerUI.runServer(p); // Starts the server
        }
    }

    /**
     * Initializes and shows the server port input UI.
     *
     * @param primaryStage the main application stage
     * @throws Exception if the FXML file or CSS fails to load
     */
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/gui/ServerPort.fxml"));

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/gui/ServerPort.css").toExternalForm());
        primaryStage.setTitle("SERVER");
        primaryStage.setScene(scene);

        primaryStage.show();
    }

    /**
     * Handles the Exit button click event.
     * Exits the application.
     *
     * @param event the action event triggered by clicking the Exit button
     * @throws Exception if shutdown encounters an error
     */
    public void getExitBtn(ActionEvent event) throws Exception {
        System.out.println("exit Academic Tool");
        System.exit(0);
    }
}
