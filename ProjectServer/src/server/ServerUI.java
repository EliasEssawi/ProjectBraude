package server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import gui.ServerMessageFrameController;
import gui.ServerPortFrameController;

/**
 * <p>
 * ServerUI is the main entry point for launching the server-side JavaFX application.
 * It opens an initial UI to input the desired port and then starts the EchoServer
 * with an associated GUI to display server logs and activity.
 * </p>
 * 
 * <p>
 * This class handles initializing and displaying the JavaFX scenes, and runs
 * the server logic on a separate thread to maintain UI responsiveness.
 * </p>
 * 
 * @author Bahaa
 */
public class ServerUI extends Application {

    /** Default port used if none is specified. */
    public static final int DEFAULT_PORT = 5555;

    /**
     * Main method that launches the JavaFX application.
     *
     * @param args Command-line arguments (not used).
     * @throws Exception if an error occurs during launch.
     */
    public static void main(String[] args) throws Exception {
        launch(args);
    }

    /**
     * Starts the JavaFX stage and loads the server port input screen.
     *
     * @param primaryStage The primary stage for this application.
     * @throws Exception if the FXML file fails to load.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        ServerPortFrameController aFrame = new ServerPortFrameController();
        aFrame.start(primaryStage);
    }

    /**
     * Initializes and runs the server on the specified port. Loads the server activity GUI
     * and starts the {@link EchoServer} in a background thread.
     *
     * @param p The port number as a String.
     */
    public static void runServer(String p) {
        int port = 0;
        try {
            port = Integer.parseInt(p);
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number.");
            return;
        }

        int finalPort = port;

        // Run GUI and server setup on the JavaFX Application Thread
        Platform.runLater(() -> {
            try {
                // Load GUI
                FXMLLoader loader = new FXMLLoader(ServerUI.class.getResource("/gui/ServerMessages.fxml"));
                Parent root = loader.load();
                ServerMessageFrameController controller = loader.getController();
                EchoServer.messageController = controller;

                // Show window
                Stage stage = new Stage();
                Scene scene = new Scene(root);
                scene.getStylesheets().add(ServerUI.class.getResource("/gui/ServerMessages.css").toExternalForm());
                stage.setTitle("Server GUI");
                stage.setScene(scene);
                stage.show();

                // Start server in a new thread AFTER controller is ready
                new Thread(() -> {
                    try {
                        EchoServer server = new EchoServer(finalPort);
                        server.listen();
                    } catch (Exception e) {
                        // Exception silently ignored (can be logged if needed)
                    	
                    }
                }).start();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        
    }
}
