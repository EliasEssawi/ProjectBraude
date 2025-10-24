package client;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

import guiUsher.AddSubController;
import guiUsher.UsherFrameController;
import gui.NotificationController;
import guiManager.ManagerFrameController;
import guiSignIn.SignInController;
import guiUser.SubscriberAwayPageController;
import guiUser.SubscriberTermenalPageController;

/**
 * The {@code ClientUI} class serves as the main entry point of the client-side JavaFX application.
 * It initializes the connection window and manages shared controller references
 * that are used across the GUI.
 * 
 * <p>Upon connection to the server, it transitions to the Sign-In page. It also handles
 * graceful client disconnection and application termination.
 * 
 * <p>This class is meant to be launched via JavaFX using {@code Application.launch()}.
 * 
 * @author Group 17
 */
public class ClientUI extends Application {

    /**
     * The only instance of the client controller (network handler).
     */
    public static ClientController chat;

    // Shared GUI controller instances for access across the application
    public static SignInController signInController;
    public static SubscriberTermenalPageController SubscriberTermenalPageController;
    public static SubscriberAwayPageController subscriberAwayController;
    public static ManagerFrameController ManagerFrameController;
    public static UsherFrameController usherFrameController;
    public static AddSubController addSubController;

    /**
     * Holds the currently logged-in user ID (for workers or users).
     * Marked TODO/DELETE as per current development state.
     */
    public static String loggedInUserID = null; // TODO: used for tracking logged in ID (e.g., by Amit)

    @FXML
    private Button btnExit = null;

    @FXML
    private Button btnConnect = null;

    @FXML
    private TextField IPField;

    @FXML
    private TextField portField;

    @FXML
    private Label lblError;

    /**
     * The entry point of the JavaFX application.
     *
     * @param args Arguments passed from the command line.
     * @throws Exception If launch fails.
     */
    public static void main(String args[]) throws Exception {
        launch(args);
    }

    /**
     * Loads and displays the initial connection window.
     * Sets up a close handler to disconnect the client gracefully.
     *
     * @param primaryStage The primary window of the JavaFX application.
     * @throws Exception If the connection frame fails to load.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            // Load and show the connection screen
            Parent root = FXMLLoader.load(getClass().getResource("/gui/ConnectionFrame.fxml"));
            Scene scene = new Scene(root);
            primaryStage.setTitle("Connection");
            primaryStage.setScene(scene);
            primaryStage.show();

            // Handle client disconnect on window close
            primaryStage.setOnCloseRequest(event -> {
                try {
                    if (chat != null && chat.isConnected()) {
                        chat.disconnect(); // Graceful disconnect from server
                    }
                } catch (IOException e) {
                    System.out.println("Error closing client connection: " + e.getMessage());
                }
            });

        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    /**
     * Connect button handler.
     * Validates IP and port fields, then attempts to create a connection to the server.
     * If successful, loads the Sign-In screen.
     *
     * @param event The JavaFX event triggered by clicking "Connect".
     * @throws Exception If connection setup fails.
     */
    public void connect(ActionEvent event) throws Exception {
        String ip = IPField.getText();
        String port = portField.getText();

        if (ip == null || ip.isEmpty())
            lblError.setText("You must fill the IP field");
        else if (port == null || port.isEmpty())
            lblError.setText("You must fill the port field");
        else {
            try {
                chat = new ClientController(ip, Integer.parseInt(port)); // Connect to server

                try {
                    // Hide the connection window
                    ((Node) event.getSource()).getScene().getWindow().hide();

                    // Load and show Sign-In window
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/guiSignIn/SignIn.fxml"));
                    Parent root = loader.load();
                    signInController = loader.getController();

                    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.setTitle("Sign In");
                    stage.show();

                } catch (IOException e) {
                    e.printStackTrace();
                }

            } catch (Exception e) {
                lblError.setText("ERROR: " + e.getMessage());
            }
        }
    }

    /**
     * Exit button handler.
     * Closes the application completely when the "Exit" button is clicked.
     *
     * @param event The JavaFX event triggered by clicking "Exit".
     * @throws Exception If termination fails.
     */
    public void getExitBtn(ActionEvent event) throws Exception {
        System.out.println("exit Academic Tool");
        System.exit(0);
    }
}
