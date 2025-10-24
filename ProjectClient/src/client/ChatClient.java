// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com 

package client;

import ocsf.client.*;
import common.ChatIF;
import common.MyFile;
import guiSignIn.SignInController;
import javafx.application.Platform;
import logic.Order;
import logic.Subscriber;

import java.io.*;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * The {@code ChatClient} class extends {@link AbstractClient} to implement
 * client-side behavior in an OCSF-based architecture.
 *
 * <p>It manages communication with the server, routes messages to the appropriate
 * controllers in the GUI, and supports both object-based and string-based messages.
 * 
 * <p>Custom logic for subscriber, manager, and usher roles is handled here.
 * 
 * @author 
 * Dr Timothy C. Lethbridge,
 * Dr Robert Laganiere,
 * Francois Belanger
 * @version July 2000 (original authorship), updated by Us Group 17 - Bahaa and team
 */
public class ChatClient extends AbstractClient {

  // Instance variables **********************************************

  /**
   * The user interface used to display messages to the client user.
   */
  ChatIF clientUI;

  /**
   * Stores an empty order instance to be shared across the client.
   */
  public static Order emptyOrder = new Order();

  /**
   * Stores the current subscriber information, shared across views.
   */
  public static Subscriber sub = new Subscriber();

  /**
   * A flag indicating whether the client is currently awaiting a server response.
   * Used to synchronize message sending and receiving.
   */
  public static boolean awaitResponse = false;

  // Constructors ****************************************************

  /**
   * Constructs a new ChatClient connected to a specified host and port,
   * and initializes the associated client UI.
   *
   * @param host The server host address.
   * @param port The server port number.
   * @param clientUI The UI to which incoming messages will be displayed.
   * @throws IOException if an error occurs while attempting to connect.
   */
  public ChatClient(String host, int port, ChatIF clientUI) throws IOException {
    super(host, port);
    this.clientUI = clientUI;
    openConnection(); // Open the socket connection
  }

  // Instance methods ************************************************

  /**
   * Handles a message received from the server. The message can be either a String
   * or a custom object (e.g., {@code MyFile}). This method determines the message
   * type and dispatches it to the appropriate controller in the UI.
   *
   * @param msg The message object received from the server.
   */
  public void handleMessageFromServer(Object msg) {
    awaitResponse = false;

    // File message (MyFile): used to send images for reports
    if (msg instanceof MyFile) {
        MyFile file = (MyFile) msg;
        String command = file.getControllerCommand();
        byte[] imageBytes = file.getFileBytes();

        Platform.runLater(() -> {
            switch (command) {
                case "ManagerFrameController SHOW_PARKING_REPORT":
                case "ManagerFrameController SHOW_SUBSCRIPTION_REPORT":
                    if (ClientUI.ManagerFrameController != null) {
                        ClientUI.ManagerFrameController.displayReportImage(imageBytes);
                    }
                    break;
                default:
                    System.out.println("Received MyFile, but command was unexpected: " + command);
            }
        });
        return;
    }

    // Text message: dispatched based on controller prefix
    if (msg instanceof String) {
        String message = ((String) msg).trim();
        ArrayList<String> parts = new ArrayList<>(Arrays.asList(message.split(" ")));
        String controller = parts.get(0).trim();

        Platform.runLater(() -> {
            switch (controller) 
            {

                // SignInController messages
                case "SignInController": {
                	String func;
                	if(parts.size() >= 3)
                	{
                    func = parts.get(1).trim() + " " + parts.get(2).trim();
                	}
                	else 
                	{
                		func = parts.get(1).trim();
                	}
                    switch (func) {
                        case "SIGN_IN_SUCCESS USERTermenal":
                        case "SIGN_IN_SUCCESS USERAway":
                        case "SIGN_IN_Fail USER":
                            if (ClientUI.signInController != null) {
                                ClientUI.signInController.display(func);
                            }
                            break;

                        case "SIGN_IN_SUCCESS Worker":
                        case "SIGN_IN_Fail Worker":
                            String type = parts.get(3).trim();
                            if (ClientUI.signInController != null) {
                                ClientUI.signInController.display(func + " " + type);
                            }
                            break;
                        case "SIGN_IN_TWICE_LOGOUT":
                        	ClientUI.signInController.display("User2Conn");
                        	break;
                        case "SIGN_IN_TWICE":
                        	ClientUI.signInController.display("Worker2Conn");
                        	break;
                        	
                    }
                    break;
                }

                // ManagerFrameController messages
                case "ManagerFrameController": {
                    if (parts.size() < 2) return;
                    String func = parts.get(1).trim();
                    String data = String.join(" ", parts.subList(2, parts.size())).trim();

                    switch (func) {
                        case "SHOW_ACTIVE_PARKINGSPOT":
                        case "SUBSCRIBERS_VIEW":
                            if (ClientUI.ManagerFrameController != null) {
                                ClientUI.ManagerFrameController.displayMsg(data);
                            }
                            break;
                    }
                    break;
                }

                // UsherFrameController messages
                case "UsherFrameController": {
                    String func = parts.get(1).trim();
                    String data = String.join(" ", parts.subList(2, parts.size())).trim();

                    switch (func) {
                        case "ADD_SUB_FAILED_EXISTS":
                        case "ADD_SUB_FAILED_INSERT":
                        case "ADD_SUB_SUCCESSFULLY":
                            if (ClientUI.usherFrameController != null) {
                                ClientUI.usherFrameController.display(func + " " + (parts.size() > 2 ? parts.get(2) : "UNKNOWN"));
                            }
                            break;

                        case "SUBSCRIBERS_VIEW":
                        case "SHOW_ACTIVE_PARKINGSPOT":
                        case "ERROR_SUBSCRIBER_ALREADY_HAS_TAG":
                        case "ERROR_NO_SUCH_SUBSCRIBER":
                        case "ERROR_INSERT_TAGREADER":
                        case "ERROR_SQL":
                            if (ClientUI.usherFrameController != null) {
                                ClientUI.usherFrameController.display(func + " " + data);
                            }
                            break;
                            
                        case "ShowHistory":
                        	 ClientUI.usherFrameController.display(func + " " + data);
                        break;
                        
                        default:
                            // Handle any case where func contains "ADD_TAG_SUCCESS"
                            if (func.contains("ADD_TAG_SUCCESS")) {
                                String[] dataParts = data.split(" ");
                                if (dataParts.length == 2) {
                                    String subscriberId = dataParts[0];
                                    String tagId = dataParts[1];
                                    if (ClientUI.usherFrameController != null) {
                                        ClientUI.usherFrameController.display("ADD_TAG_SUCCESS" + " " + subscriberId + " " + tagId);
                                    }
                                } else {
                                    if (ClientUI.usherFrameController != null) {
                                        ClientUI.usherFrameController.display(func + " (Missing data)");
                                    }
                                }
                            } else {
                                // All other unknown cases - send raw
                                if (ClientUI.usherFrameController != null) {
                                    ClientUI.usherFrameController.display(func + " " + data);
                                }
                            }
                            break;

                    }
                    break;
                }

                // SubscriberAwayController messages
                case "SubscriberAwayController": {
                    String status = parts.get(1).trim();
                    switch (status) {
                        case "CHECK_SPOTS_FAIL":
                            ClientUI.subscriberAwayController.display("Error CHECK_SPOTS_FAIL");
                            break;

                        case "CHECK_SPOTS_SUCCESS":
                            ClientUI.subscriberAwayController.display("CHECK_SPOTS_SUCCESS " + parts.get(2).trim());
                            break;

                        case "RESERVE_SUCCESS":
                            ClientUI.subscriberAwayController.display("RESERVE_SUCCESS " + parts.get(2).trim());
                            break;

                        case "RESERVE_TIME_FAIL":
                        case "RESERVE_TIME_ERROR":
                        case "RESERVE_FAIL":
                        case "RESERVE_Exist":
                            ClientUI.subscriberAwayController.display(status);
                            break;

                        case "SHOW_SUBSCRIBER_HISTORY": {
                            String history = String.join(" ", parts.subList(2, parts.size())).trim();
                            ClientUI.subscriberAwayController.displayHistory(history);
                            break;
                        }

                        case "SHOW_PERSONAL_DATA":
                            ClientUI.subscriberAwayController.displayPersonalData(message);
                            break;

                        case "UPDATE_SUCCESS":
                        case "UPDATE_FAIL":
                            ClientUI.subscriberAwayController.showUpdateResult(status.equals("UPDATE_SUCCESS"));
                            break;

                        case "EXTEND_PARKING_RESULT": {
                            String extendResult = String.join(" ", parts.subList(2, parts.size())).trim();
                            ClientUI.subscriberAwayController.display(extendResult);
                            break;
                        }

                        case "ACTIVE_PARKING":
                        case "NO_ACTIVE_PARKING":
                            ClientUI.subscriberAwayController.display(status.replace('_', ' '));
                            break;
                    }
                    break;
                }

                // SubscriberTermenalController messages
                case "SubscriberTermenalController": {
                    String status = parts.get(1).trim();
                    switch (status) {
                        case "CHECK_SPOTS_FAIL":
                            ClientUI.SubscriberTermenalPageController.display("Error CHECK_SPOTS_FAIL");
                            break;

                        case "CHECK_SPOTS_SUCCESS":
                        case "GET_PARKING_CODE_SUCCESS":
                        case "GET_PARKING_CODE_WARNING":
                            ClientUI.SubscriberTermenalPageController.display(status + " " + (parts.size() > 2 ? parts.get(2).trim() : "DATA_MISSING"));
                            break;

                        case "GET_PARKING_CODE_FAIL":
                            ClientUI.SubscriberTermenalPageController.display("Error GET_PARKING_CODE_FAIL");
                            break;

                        case "ParkWithReservation": {
                            String parkStatus = parts.get(2).trim();
                            String details = (parts.size() > 3) ? String.join(" ", parts.subList(3, parts.size())) : "";
                            ClientUI.SubscriberTermenalPageController.display(parkStatus + " " + details);
                            break;
                        }

                        case "SHOW_SUBSCRIBER_HISTORY":
                            String history = String.join(" ", parts.subList(2, parts.size()));
                            ClientUI.SubscriberTermenalPageController.displayHistory(history);
                            break;

                        case "GET_PARKING_STATUS_SUCCESS":
                            if (parts.size() == 6) {
                                String msgStat = String.join(" ", parts.subList(1, 6));
                                ClientUI.SubscriberTermenalPageController.display(msgStat);
                            } else {
                                ClientUI.SubscriberTermenalPageController.display("Error GET_PARKING_STATUS_WARNING_DATA_MISSING");
                            }
                            break;

                        case "GOT_EMPTY_STATUS":
                        case "RETRIEVING_CAR_SUCCESS":
                            ClientUI.SubscriberTermenalPageController.display(status);
                            break;

                        case "RETRIEVING_CAR_FAILED":
                            ClientUI.SubscriberTermenalPageController.display("Error RETRIEVING_CAR_FAILED");
                            break;

                        case "NO_CAR_TO_RETRIEVE":
                            ClientUI.SubscriberTermenalPageController.display("Error NO_CAR_TO_RETRIEVE");
                            break;
                    }
                    break;
                }

                // Default: Try sending to any known controller
                default:
                    if (ClientUI.signInController != null)
                        ClientUI.signInController.display(message);
                    if (ClientUI.SubscriberTermenalPageController != null)
                        ClientUI.SubscriberTermenalPageController.display(message);
                    if (ClientUI.subscriberAwayController != null)
                        ClientUI.subscriberAwayController.display(message);
                    if (ClientUI.ManagerFrameController != null)
                        ClientUI.ManagerFrameController.displayMsg(message);
                    if (ClientUI.usherFrameController != null)
                        ClientUI.usherFrameController.display(message);
            }
        });
    }
  }

  /**
   * Sends a message from the UI and waits for the server to respond.
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

  /**
   * Sends a list-based message to the server and waits for a response.
   *
   * @param message A list of strings to send to the server.
   */
  public void handleMessageArrayFromClientUI(ArrayList<String> message) {
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
      clientUI.display("Could not send array message to server: Terminating client." + e);
      quit(); // Close client if sending fails
    }
  }

  /**
   * Called when an exception is thrown while trying to communicate with the server.
   * Displays an error and terminates the client after 5 seconds.
   *
   * @param exception The exception encountered.
   */
  @Override
  protected void connectionException(Exception exception) {
    clientUI.display("Lost connection to the server.");
    try {
      Thread.sleep(5000); // Wait 5 seconds before exiting
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    quit();
  }

  /**
   * Gracefully disconnects the client from the server and exits the application.
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
