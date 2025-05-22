// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com 

package client;

import ocsf.client.*;
import common.ChatIF;
import gui.OrderFormController;
import javafx.application.Platform;
import logic.Order;

import java.io.*;
import java.sql.Date;
import java.util.ArrayList;

/**
 * This class extends AbstractClient to provide functionality specific to the client side
 * of a client-server application, particularly for handling chat and order-related messages.
 *
 * @author Dr Timothy C. Lethbridge
 * @author Dr Robert Laganière
 * @author François Bélanger
 * @version July 2000
 */
public class ChatClient extends AbstractClient {

  // Instance variables **********************************************

  /**
   * The UI object that displays messages to the user.
   */
  ChatIF clientUI;

  /**
   * Stores the current order retrieved from the server.
   */
  public static Order emptyOrder = new Order();

  /**
   * Flag to indicate if the client is waiting for a server response.
   */
  public static boolean awaitResponse = false;

  // Constructors ****************************************************

  /**
   * Constructs a ChatClient instance and connects to the specified host and port.
   *
   * @param host The server host to connect to.
   * @param port The server port to connect to.
   * @param clientUI The user interface that will display messages.
   * @throws IOException if connection fails.
   */
  public ChatClient(String host, int port, ChatIF clientUI) throws IOException {
    super(host, port); // Call the constructor from AbstractClient
    this.clientUI = clientUI;
    openConnection();  // Open the socket connection
  }

  // Instance methods ************************************************

  /**
   * This method handles messages received from the server.
   *
   * @param msg The message received from the server.
   */
  public void handleMessageFromServer(Object msg) {
    awaitResponse = false; // Mark that response has been received
    handleMessageUpdate(msg); // Handle UI updates or error codes

    String st = msg.toString();
    String[] result = st.split("\\s");

    // Expecting 6 parts for a valid Order
    if (result.length != 6) {
      return; // Not a valid order format
    }

    // Parse and populate the Order object
    emptyOrder.setParkingSpace(Integer.parseInt(result[0]));
    emptyOrder.setOrderNumber(Integer.parseInt(result[1]));
    emptyOrder.setOrderDate(Date.valueOf(result[2]));
    emptyOrder.setConfirmationCode(Integer.parseInt(result[3]));
    emptyOrder.setSubscriberId(Integer.parseInt(result[4]));
    emptyOrder.setDateOfPlacingAnOrder(Date.valueOf(result[5]));
  }

  /**
   * Handles special message updates such as errors, runs on JavaFX UI thread.
   *
   * @param msg The server message to process.
   */
  protected void handleMessageUpdate(Object msg) {
    if (!(msg instanceof String)) return;

    String message = (String) msg;

    Platform.runLater(() -> {
      // If the server sent an ORDER_ERROR, show it in the form
      if (message.startsWith("ORDER_ERROR:")) {
        String cleanMsg = message.replace("ORDER_ERROR:", "").trim();
        OrderFormController.showErrorMessage(cleanMsg);
      }
      // Additional message types can be handled here if needed
    });
  }

  /**
   * Handles input messages from the UI and sends them to the server.
   *
   * @param message The message string entered by the user.
   */
  public void handleMessageFromClientUI(String message) {
    try {
      openConnection(); // Ensure connection is open before sending
      awaitResponse = true;
      sendToServer(message); // Send message to server

      // Wait for the server to respond before continuing
      while (awaitResponse) {
        try {
          Thread.sleep(100); // Wait loop
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
      clientUI.display("Could not send message to server: Terminating client." + e);
      quit(); // Close client if sending fails
    }
  }
  public void handleMessageArrayFromClientUI(ArrayList<String> message) {
	    try {
	      openConnection(); // Ensure connection is open before sending
	      awaitResponse = true;
	      sendArrayToServer(message); // Send message to server

	      // Wait for the server to respond before continuing
	      while (awaitResponse) {
	        try {
	          Thread.sleep(100); // Wait loop
	        } catch (InterruptedException e) {
	          e.printStackTrace();
	        }
	      }
	    } catch (IOException e) {
	      e.printStackTrace();
	      clientUI.display("Could not send Array message to server: Terminating client." + e);
	      quit(); // Close client if sending fails
	    }
	  }
  @Override
  protected void connectionException(Exception exception) {
      clientUI.display("Lost connection to the server.");
      try {
          Thread.sleep(5000); //wait 5 seconds
      } catch (InterruptedException e) {
          e.printStackTrace();
      }
      quit(); // סוגר את החיבור והאפליקציה
  }


  /**
   * Gracefully shuts down the client connection and exits the program.
   */
  public void quit() {
    try {
      closeConnection();
    } catch (IOException e) {
      // Ignore exception on close
    }
    System.exit(0);
  }
}

// End of ChatClient class
