package server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ocsf.server.ConnectionToClient;

import java.util.Vector;

import gui.ServerMessageFrameController;
import gui.ServerPortFrameController;
import server.EchoServer;

public class ServerUI extends Application {
	final public static int DEFAULT_PORT = 5555;
	//public static Vector<Student> students=new Vector<Student>();

	// Launches the JavaFX application
	public static void main(String args[]) throws Exception {   
		launch(args);
	} // end main
	
	// Starts the initial server port input window
	@Override
	public void start(Stage primaryStage) throws Exception {
		// create ServerFrame		 
		ServerPortFrameController aFrame = new ServerPortFrameController();
		aFrame.start(primaryStage);
	}
	
	// Runs the server GUI and starts the EchoServer on the given port
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
	    javafx.application.Platform.runLater(() -> {
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

	            // Start server in new thread AFTER controller is ready
	            new Thread(() -> {
	                try {
	                    EchoServer server = new EchoServer(finalPort);
	                    server.listen();
	                } catch (Exception e) {
	                   
	                }
	            }).start();

	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    });
	} 
}
