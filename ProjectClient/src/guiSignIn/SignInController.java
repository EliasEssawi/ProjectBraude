package guiSignIn;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;

import client.ClientUI;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.Label;

/**
 * Controller class for the Sign In screen.
 * 
 * <p>This class handles user and worker login options, sending sign-in requests
 * to the server, displaying errors, and routing users to their respective dashboards.
 * 
 * <p>It supports multiple login modes: User Terminal, User Away, Usher, Manager, and Tag-based.
 * 
 * @author Group 17
 */
public class SignInController {

    /** Stage reference for login window to allow closing it later */
    private static Stage loginStage;

    /** (Unused) Index for selection logic */
    private static int itemIndex = 3;

    // Buttons and text fields for user interactions
    @FXML private Button btnExit;
    @FXML private Button btnUser;
    @FXML private Button btnWorker;
    @FXML private Label lblNotification;

    @FXML private TextField txtUsherSignIn;
    @FXML private TextField txtUserSignIn;
    @FXML private TextField txtManagerSignIn;
    @FXML private TextField txtUserNameSignIn; /** @author Amit_Regev */

    @FXML private VBox Sections;
    @FXML private Button btnManager;
    @FXML private Button btUsher;
    @FXML private Button btUser;

    @FXML private VBox vboxAsUser;
    @FXML private VBox vboxAsUsher;
    @FXML private VBox vboxAsManager;

    @FXML private Button btnSubscriberAway;
    @FXML private VBox vboxAsSubscriberAway;
    @FXML private TextField txtSubscriberAwaySignIn;
    @FXML private TextField txtSubscriberAwayNameSignIn; /** @author Amit_Regev */

    @FXML private VBox vboxAsSubscriberTag;
    @FXML private TextField txtSubscriberSignInTag;

    /** Sets reference to the login stage so it can be closed after login */
    public void setLoginStage(Stage stage) {
        loginStage = stage;
    }

    /** Closes login stage if it is still open */
    public static void closeLoginStageIfOpen() {
        if (loginStage != null) {
            loginStage.close();
            loginStage = null;
        }
    }

    /** 
     * Opens the UI section for Subscriber Away login
     */
    @FXML
    private void userSignInAwayOption(ActionEvent event) {
        HideAllSectionExept(this.vboxAsSubscriberAway);
    }

    /** 
     * Opens the UI section for Subscriber Tag login
     */
    @FXML
    private void userSignInByTag(ActionEvent event) {
        HideAllSectionExept(this.vboxAsSubscriberTag);
    }

    /**
     * Handles subscriber sign in by tag reader ID.
     */
    @FXML
    private void subscriberSignInTag(ActionEvent event) throws IOException {
        String tagReaderId = txtSubscriberSignInTag.getText().trim();
        if (tagReaderId.isEmpty()) {
            lblNotification.setText("Please enter a TagReader ID.");
            lblNotification.setVisible(true);
            lblNotification.setManaged(true);
            return;
        }

        ArrayList<String> msg = new ArrayList<>();
        msg.add("tagreader sign in");
        msg.add(tagReaderId);
        ClientUI.chat.acceptArray(msg);
    }

    /** Displays subscriber login section */
    @FXML
    private void userSignInOption(ActionEvent event) {
        HideAllSectionExept(this.vboxAsUser);
    }

    /** Displays usher login section */
    @FXML
    private void usherSignInOption(ActionEvent event) {
        HideAllSectionExept(this.vboxAsUsher);
    }

    /** Displays manager login section */
    @FXML
    private void managerSignInOption(ActionEvent event) {
        HideAllSectionExept(this.vboxAsManager);
    }

    /** Sends manager sign in request to the server */
    @FXML
    private void managerSignIn(ActionEvent event) throws IOException {
        ArrayList<String> str = new ArrayList<>();
        str.add("worker sign in");
        str.add(txtManagerSignIn.getText());
        str.add("1"); // Manager role
        ClientUI.chat.acceptArray(str);
    }

    /** Sends user sign in request (terminal) */
    @FXML
    private void userSignIn(ActionEvent event) throws IOException {
        ArrayList<String> str = new ArrayList<>();
        String userID = txtUserSignIn.getText();
        String userName = txtUserNameSignIn.getText().trim();
        str.add("user sign in");
        str.add(userID);
        str.add(userName);
        ClientUI.loggedInUserID = userID;
        ClientUI.chat.acceptArray(str);
    }

    /** Sends away user sign in request (not from terminal) */
    @FXML
    private void subscriberAwaySignIn(ActionEvent event) throws IOException {
        ArrayList<String> str = new ArrayList<>();
        String userID = txtSubscriberAwaySignIn.getText();
        String userName = txtSubscriberAwayNameSignIn.getText().trim();
        str.add("user sign in away");
        str.add(userID);
        str.add(userName);
        ClientUI.loggedInUserID = userID;
        ClientUI.chat.acceptArray(str);
    }

    /** Sends usher sign in request to server */
    @FXML
    private void usherSignIn(ActionEvent event) throws IOException {
        ArrayList<String> str = new ArrayList<>();
        str.add("worker sign in");
        str.add(txtUsherSignIn.getText());
        str.add("0"); // Usher role
        ClientUI.chat.acceptArray(str);
    }

    /**
     * Utility method to hide all login form sections except the selected one
     * @param section The VBox section to show
     */
    private void HideAllSectionExept(VBox section) {
        ObservableList<Node> allSections = Sections.getChildren();
        for (Node s : allSections) {
            s.setVisible(s == section);
            s.setManaged(s == section);
        }
    }

    /**
     * Dynamically loads a new page and assigns its controller
     * to the appropriate field in ClientUI for later access.
     */
    public void loadPage(String fxmlPath, String cssPath, String title, String controllerName) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();

        switch (controllerName) {
            case "subscriberAwayController":
                ClientUI.subscriberAwayController = loader.getController();
                break;
            case "SubscriberTermenalPageController":
                ClientUI.SubscriberTermenalPageController = loader.getController();
                break;
            case "ManagerFrameController":
                ClientUI.ManagerFrameController = loader.getController();
                break;
            case "UsherFrameController":
                ClientUI.usherFrameController = loader.getController();
                break;
            case "AddSubController":
                ClientUI.addSubController = loader.getController();
                break;
            default:
                break;
        }

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(scene);
        stage.show();
    }

    /** Handles exit button click - disconnects and exits program */
    public void getExitBtn(ActionEvent event) throws Exception {
        System.out.println("exit Academic Tool");
        ClientUI.chat.disconnect();
        Thread.sleep(500);
        System.exit(0);
    }

    /**
     * Called by the server response handler to process login success/failure
     */
    public void display(String message) {
        Platform.runLater(() -> {
            try {
                if (message.equals("SIGN_IN_SUCCESS USERAway")) {
                    loadPage("/guiUser/SubscriberAwayPage.fxml", "/guiUser/SubscriberAwayPage.css", "User Dashboard(Away)", "subscriberAwayController");
                    closeCurrentWindow();
                } else if (message.equals("SIGN_IN_SUCCESS USERTermenal")) {
                    loadPage("/guiUser/SubscriberTermenalPage.fxml", "/guiUser/SubscriberTermenalPage.css", "User Dashboard(Termenal)", "SubscriberTermenalPageController");
                    closeCurrentWindow();
                } else if (message.startsWith("SIGN_IN_SUCCESS Worker")) {
                    String[] parts = message.split(" ");
                    if (parts.length == 3) {
                        String workerType = parts[2];
                        if ("1".equals(workerType)) {
                            loadPage("/guiManager/ManagerFrame.fxml", "/guiManager/ManagerFrame.css", "Manager Dashboard", "ManagerFrameController");
                            closeCurrentWindow();
                        } else if ("0".equals(workerType)) {
                            loadPage("/guiUsher/UsherFrame.fxml", "/guiUsher/UsherFrame.css", "Usher Dashboard", "UsherFrameController");
                            closeCurrentWindow();
                        }
                    }
                } else if (message.startsWith("SIGN_IN_Fail")) {
                    if (message.contains("USER")) {
                        lblNotification.setText("Failed, Subscriber ID or name is not found.");
                    } else if (message.contains("Worker 0")) {
                        lblNotification.setText("Usher Login failed. Please check your ID.");
                    } else {
                        lblNotification.setText("Manager Login failed. Please check your ID.");
                    }
                }
                else if(message.contains("Worker2Conn"))
                {
                	 lblNotification.setText("One Connection is allowed for workers!");
                } 
                else if(message.contains("User2Conn"))
                {
                	
                	lblNotification.setText("Two Connections for users disallowed!"); 

                } 
                else {
                    lblNotification.setText("Error");
                }
                lblNotification.setVisible(true);
                lblNotification.setManaged(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /** Closes the currently active window (used after navigation) */
    private void closeCurrentWindow() {
        Stage stage = (Stage) Sections.getScene().getWindow();
        stage.close();
    }

    /** Overload: Closes current window using event source */
    private void closeCurrentWindow(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }
}
