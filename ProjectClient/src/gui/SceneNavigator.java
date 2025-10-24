package gui; // or any other package used consistently in your project

import java.io.IOException;

import client.ClientUI;
import guiSignIn.SignInController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * The {@code SceneNavigator} class provides utility methods
 * for navigating between JavaFX scenes in the application.
 *
 * <p>Currently, it includes logic for logging out and returning to the login screen,
 * and can be extended to support more scene transitions throughout the system.
 * 
 * <p>This helps maintain separation of concerns and reuse of scene-loading logic.
 * 
 * @author Group 17
 */
public class SceneNavigator {

    /**
     * Logs out the current user and returns to the login screen.
     *
     * <p>This method loads the {@code SignIn.fxml} file, applies the corresponding CSS stylesheet,
     * creates a new scene, and sets it on the current stage.
     * 
     * <p>It also updates the {@code ClientUI.signInController} reference with the new controller
     * instance, allowing it to be accessed after the screen is reloaded.
     *
     * @param currentStage The currently open stage (window) that will be reused for the login screen.
     */
    public static void logoutAndReturnToLogin(Stage currentStage) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource("/guiSignIn/SignIn.fxml"));
            Parent root = loader.load();
            ClientUI.signInController = loader.getController();

            Scene scene = new Scene(root);
            scene.getStylesheets().add(SceneNavigator.class.getResource("/guiSignIn/SignIn.css").toExternalForm());

            currentStage.setScene(scene);
            currentStage.setTitle("Sign In");
            currentStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
