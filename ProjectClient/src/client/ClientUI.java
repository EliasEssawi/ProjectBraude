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

import gui.WorkerFrameController;
import gui.OrderFormController;
import client.ClientController;

public class ClientUI extends Application {
	public static ClientController chat; // only one instance
	public static OrderFormController currentOrderFormController;

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

	// Main method to launch the JavaFX application
	public static void main(String args[]) throws Exception { 
	    launch(args);  
	} // end main
	 
	// Start method to display the initial connection window
	@Override
	public void start(Stage primaryStage) throws Exception {
		try {
			// show connection page
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
	
	// Connect button handler - attempts connection and opens worker frame
	public void connect(ActionEvent event) throws Exception {
		String ip = IPField.getText();
		String port = portField.getText();
		
		if (ip.equals("") || ip == null)
			lblError.setText("You must fill the IP field");
		else if (port.equals("") || port == null)
			lblError.setText("You must fill the port field");
		else {
			try {
				chat = new ClientController(ip, Integer.parseInt(port));
				
				try {
				 	((Node)event.getSource()).getScene().getWindow().hide(); // hiding primary window

			        Parent newRoot = FXMLLoader.load(getClass().getResource("/gui/WorkerFrame.fxml")); // Target FXML file

			        // Get current stage from event
			        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
			        stage.setScene(new Scene(newRoot));
			        stage.show();
			        
			    } catch (IOException e) {
			        e.printStackTrace();
			    }
			} catch (Exception e) {
				lblError.setText("ERROR: " + e.getMessage());
			}
		}
	}
	
	// Exit button handler - terminates the application
	public void getExitBtn(ActionEvent event) throws Exception {
		System.out.println("exit Academic Tool");	
		System.exit(0);
	}
}
