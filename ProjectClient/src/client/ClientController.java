// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com 

package client;

import java.io.*;
import java.util.ArrayList;

import common.ChatIF;

/**
 * This class constructs the UI for a chat client.  
 * It implements the ChatIF interface in order to activate the display() method.
 * Warning: Some of the code here is cloned in ServerConsole.
 *
 * @author François Bélanger
 * @author Dr Timothy C. Lethbridge  
 * @author Dr Robert Laganière
 * @version July 2000
 */
public class ClientController implements ChatIF 
{
  // *************************************************
  // Class Variables
  // *************************************************

  /**
   * The default port to connect on.
   */
  public static int DEFAULT_PORT;

  // *************************************************
  // Instance Variables
  // *************************************************

  /**
   * The instance of the client that this controller wraps.
   */
  ChatClient client;

  // *************************************************
  // Constructors
  // *************************************************

  /**
   * Constructs an instance of the ClientController UI.
   *
   * @param host The host to connect to (e.g., "localhost").
   * @param port The port to connect on (e.g., 5555).
   * @throws IOException If the client connection fails.
   */
  public ClientController(String host, int port) throws IOException {
    try {
      // Initialize ChatClient and pass this controller for message display
      client = new ChatClient(host, port, this);
    } catch(IOException exception) {
      // If connection setup fails, throw an exception
      throw new IOException("Error: Can't setup connection!");
    }
  }

  // *************************************************
  // Instance Methods
  // *************************************************

  /**
   * This method sends input from the UI to the client's handler.
   *
   * @param str The message to send to the server.
   */
  public void accept(String str) {
    client.handleMessageFromClientUI(str);
  }
  public void acceptArray(ArrayList<String> str) {
	    client.handleMessageArrayFromClientUI(str);
	  }

  /**
   * Implements the display method from ChatIF interface.
   * Displays a message on the client screen.
   *
   * @param message The message string to display.
   */
  public void display(String message) {
    System.out.println(">-- " + message);
  }

  /**
   * Disconnects the client from the server safely.
   */
  public void disconnect() throws IOException {
    try {
      client.closeConnection(); // Closes the socket connection
    } catch (Exception e) {
      System.out.println("Error while disconnecting: " + e.getMessage());
    }
  }

  /**
   * Checks whether the client is currently connected to the server.
   *
   * @return true if connected, false otherwise
   */
  public boolean isConnected() {
    return client != null && client.isConnected();
  }
}

// End of ClientController class
