package gui;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
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
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import server.EchoServer;
import server.ServerUI;
import javafx.scene.control.TextArea;
import javafx.application.Platform;

public class ServerMessageFrameController  {
	
	private EchoServer server;
	
	String temp="";
	
	@FXML
	private Button btnExit = null;
	@FXML
	private Button btnDone = null;
	@FXML
	private Label lbllist;
	
	@FXML
	private TextField portxt;
	@FXML
	public  TextArea LogArea;

	@FXML
	private Label statusLabel;
	ObservableList<String> list;
	@FXML
	private TextArea logArea;

	// Sets the EchoServer instance for this controller
	public void setServer(EchoServer server) {
	    this.server = server;
	}

	// Appends a message to the log area (UI-safe)
	public void appendMessage(String msg) {
	    Platform.runLater(() -> {
	        logArea.appendText(msg + "\n");
	    });
	}

	// Returns the entered port from the text field
	private String getport() {
		return portxt.getText();			
	}

	// Starts the server GUI, initializes EchoServer, and binds it to the controller
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

	// Handles the exit button: stops the server and exits the application
	@FXML
	public void getExitBtn(ActionEvent event) {
	    System.out.println("Exit Academic Tool");

	    try {
	        if (server != null) {
	            server.close();
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
