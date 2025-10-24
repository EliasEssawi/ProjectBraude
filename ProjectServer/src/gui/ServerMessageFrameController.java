package gui;

import java.io.IOException;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import server.EchoServer;

/**
 * <p>
 * Controller for the server GUI, responsible for initializing and managing
 * the server instance (`EchoServer`) and providing a user interface for
 * monitoring server activity and status messages.
 * </p>
 * 
 * <p>
 * Includes functionality to start the server, display log messages,
 * retrieve the selected port, and shut down the server.
 * </p>
 * 
 * @author Bahaa
 */
public class ServerMessageFrameController {

    /** The EchoServer instance used by this controller */
    private EchoServer server;

    /** Temporary string for internal usage (not currently used) */
    String temp = "";

    /** Button to exit the server GUI */
    @FXML
    private Button btnExit = null;

    /** Button to apply/start server (not used here) */
    @FXML
    private Button btnDone = null;

    /** Label showing list-related text (not used here) */
    @FXML
    private Label lbllist;

    /** Text field for entering the server port */
    @FXML
    private TextField portxt;

    /** TextArea displaying server log messages (duplicate of `logArea`) */
    @FXML
    public TextArea LogArea;

    /** Label to show server status updates */
    @FXML
    private Label statusLabel;

    /** Observable list (not used currently) */
    ObservableList<String> list;

    /** Main TextArea for displaying server logs */
    @FXML
    private TextArea logArea;

    /**
     * Sets the {@link EchoServer} instance used by this controller.
     * 
     * @param server the EchoServer instance
     */
    public void setServer(EchoServer server) {
        this.server = server;
    }

    /**
     * Appends a log message to the server log area (thread-safe).
     *
     * @param msg the message to append
     */
    public void appendMessage(String msg) {
        Platform.runLater(() -> {
            logArea.appendText(msg + "\n");
        });
    }

    /**
     * Retrieves the port number entered in the text field.
     *
     * @return the entered port number as a String
     */
    private String getport() {
        return portxt.getText();
    }

    /**
     * Launches the server GUI, initializes the {@link EchoServer},
     * binds it to this controller, and starts listening on port 5555.
     *
     * @param primaryStage the JavaFX stage for the window
     * @throws Exception if FXML loading or server startup fails
     */
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ServerMessages.fxml"));
        Parent root = loader.load();

        ServerMessageFrameController controller = loader.getController();

        EchoServer server = new EchoServer(5555);
        EchoServer.messageController = controller;
        server.listen();

        controller.setServer(server);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Server Activity");
        primaryStage.show();
    }

    /**
     * Handles the exit button action.
     * Attempts to shut down the server and terminate the application.
     *
     * @param event the ActionEvent triggered by clicking the Exit button
     */
    @FXML
    public void getExitBtn(ActionEvent event) {
        System.out.println("Exit Academic Tool");

        try {
            if (server != null) {
                server.shutdownServer(); // Close DB connection
                server.close(); // Stop listening to new connections
                System.out.println("Server stopped.");
            } else {
                System.out.println("Server is null.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.exit(0);
    }
}
