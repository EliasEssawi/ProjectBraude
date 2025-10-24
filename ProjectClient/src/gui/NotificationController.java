package gui;

import javafx.animation.PauseTransition;
import javafx.scene.control.Label;
import javafx.util.Duration;

/**
 * The {@code NotificationController} class provides a utility to display 
 * temporary notification messages in a JavaFX application using a {@link Label}.
 * 
 * <p>Notifications can be styled based on their type (e.g., success, error, info),
 * and will automatically disappear after a specified duration.
 * 
 * <p>This class is especially useful for providing real-time feedback to users
 * after performing actions such as submitting forms, saving data, or handling errors.
 * 
 * @author Elias
 */
public class NotificationController {

    /**
     * Represents different types of notifications.
     * Each type corresponds to a different background color for the label.
     */
    public enum NotificationType {
        /** Operation completed successfully. */
        SUCCESS,

        /** Neutral informational message. */
        INFO,

        /** Cautionary alert or warning message. */
        WARNING,

        /** Indicates an error or failure. */
        ERROR,

        /** Developer-level debug message. */
        DEBUG,
    }

    /**
     * Displays a styled, temporary notification message on a given {@link Label}.
     * 
     * @param label The {@code Label} where the message will be shown.
     * @param message The text content of the notification.
     * @param durationSeconds Duration (in seconds) before the message disappears.
     * @param type The {@link NotificationType} used to determine the background color.
     */
    public static void showNotification(Label label, String message, int durationSeconds, NotificationType type) {
        label.setText(message);
        label.setVisible(true);
        
        // Set background color based on the type of notification
        switch (type) {
            case SUCCESS:
                label.setStyle("-fx-background-color: rgba(0,255,0,0.5);"); // Green
                break;
            case ERROR:
                label.setStyle("-fx-background-color: rgba(176, 35, 35,0.5);"); // Red
                break;
            case WARNING:
                label.setStyle("-fx-background-color: rgba(235, 180, 52,0.5);"); // Yellow/Orange
                break;
            case INFO:
                label.setStyle("-fx-background-color: rgba(77, 79, 82,0.5);"); // Gray
                break;
            case DEBUG:
                label.setStyle("-fx-background-color: rgba(255, 165, 0, 0.5);"); // Orange
                break;
        }

        // Schedule label reset after the duration
        PauseTransition pause = new PauseTransition(Duration.seconds(durationSeconds));
        pause.setOnFinished(event -> {
            label.setText("");       // Clear the message
            label.setVisible(false); // Optionally hide the label
        });
        pause.play();
    }
}
