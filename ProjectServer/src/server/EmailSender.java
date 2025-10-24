/**
 * <p>EmailSender handles sending HTML-formatted emails for various BPARK system events.</p>
 * <p>Currently supports sending:</p>
 * <ul>
 *   <li>Late vehicle pickup notifications</li>
 *   <li>Parking access code deliveries</li>
 * </ul>
 * <p>Uses Gmail SMTP server and JavaMail API with preconfigured credentials.</p>
 * 
 * @author Bahaa
 */
package server;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class EmailSender {

    private static final String GMAIL_USERNAME = "bparkserviceg17@gmail.com";
    private static final String GMAIL_APP_PASSWORD = "wwtzzkaipyrtmcma";
    private static final String SUPPORT_PHONE = "04-000000";
    private static final String SUPPORT_EMAIL = "bparkserviceg17@gmail.com";

    /**
     * Sends an HTML-formatted email asynchronously using a background thread.
     * <p>
     * This method allows the main application to continue executing without waiting
     * for the email to be sent. It is useful in UI or server environments where responsiveness
     * is critical, and blocking calls to the SMTP server should be avoided.
     * </p>
     *
     * @param to      The recipient's email address.
     * @param subject The subject line of the email.
     * @param html    The HTML-formatted content of the email body.
     */
    public static void sendEmailAsync(String to, String subject, String html) {
        new Thread(() -> sendEmail(to, subject, html)).start();
    }
    
    /**
     * Sends an email notifying the user of a late pickup.
     * 
     * @param recipientEmail The recipient's email address
     * @param customerName   The name of the customer
     */
    public static void sendLatePickupEmail(String recipientEmail, String customerName) {
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        sendEmailAsync(
            recipientEmail,
            "Pick up your car! Time Expired " + currentDate,
            createLatePickupEmailTemplate(customerName, currentDate, currentTime)
        );
    }

    /**
     * Creates the HTML content for the late pickup email.
     * 
     * @param customerName Name of the customer
     * @param date         Current date
     * @param time         Current time
     * @return Formatted HTML content
     */
    private static String createLatePickupEmailTemplate(String customerName, String date, String time) {
        return """
            <html>
                <body style=\"font-family: Arial, sans-serif; line-height: 1.6;\">
                    <h2 style=\"color: #D9534F;\">Late Vehicle Pickup Notification</h2>
                    <p>Hello <strong>%s</strong>,</p>
                    <p>The BPARK system has detected that your parking session has exceeded the allowed limit.</p>
                    <ul>
                        <li><strong>Date:</strong> %s</li>
                        <li><strong>Current Time:</strong> %s</li>
                    </ul>
                    <p>Please contact us for support or to resolve the delay.</p>
                    <p>Thank you,<br>The BPARK Team</p>
                    <hr>
                    <small>Contact us: %s | %s</small>
                </body>
            </html>
            """.formatted(customerName, date, time, SUPPORT_PHONE, SUPPORT_EMAIL);
    }
    /**
     * Sends an email notifying the user that their car was forcibly removed after exceeding parking time.
     *
     * @param recipientEmail The recipient's email address
     * @param customerName   The name of the customer
     * @param exitTime       The time the car was forced out
     */
    public static void sendForcedExitEmail(String recipientEmail, String customerName, String exitTime) {
        String subject = "Notice: Your Vehicle Was Removed from the Parking Lot";
        String htmlContent = createForcedExitEmailTemplate(customerName, exitTime);
        sendEmailAsync(recipientEmail, subject, htmlContent);
    }

    /**
     * Creates the HTML content for the forced exit email.
     *
     * @param customerName Name of the customer
     * @param exitTime     The time the car was removed
     * @return Formatted HTML content
     */
    private static String createForcedExitEmailTemplate(String customerName, String exitTime) {
        return """
            <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6;">
                    <h2 style="color: #d9534f;">Parking Exit Notification</h2>
                    <p>Hello <strong>%s</strong>,</p>
                    <p>This is to inform you that your vehicle was automatically removed from the parking lot by the BPARK system.</p>
                    <p>This action was taken because your parking session exceeded the allowed time limit by 4 hours.</p>
                    <ul>
                        <li><strong>Exit Time:</strong> %s</li>
                    </ul>
                    <p>If you believe this was a mistake or if you have any questions, please contact our support team.</p>
                    <p>Thank you,<br>The BPARK Team</p>
                    <hr>
                    <small>Contact us: %s | %s</small>
                </body>
            </html>
            """.formatted(customerName, exitTime, SUPPORT_PHONE, SUPPORT_EMAIL);
    }


    /**
     * Sends an email containing a parking access code.
     * 
     * @param recipientEmail The recipient's email
     * @param customerName   The customer's name
     * @param code           The parking access code
     */
    public static void sendParkingCodeEmail(String recipientEmail, String customerName, String code) {
        String subject = "Your BPARK Parking Code";
        String htmlContent = """
            <html>
                <body style=\"font-family: Arial, sans-serif; line-height: 1.6;\">
                    <h2 style=\"color: #5cb85c;\">Parking Code Access</h2>
                    <p>Hello <strong>%s</strong>,</p>
                    <p>Here is your parking access code:</p>
                    <p style=\"font-size: 20px;\"><strong>%s</strong></p>
                    <p>Please use it to access your parking reservation.</p>
                    <p>Thank you,<br>The BPARK Team</p>
                    <hr>
                    <small>Contact us: %s | %s</small>
                </body>
            </html>
            """.formatted(customerName, code, SUPPORT_PHONE, SUPPORT_EMAIL);

        sendEmailAsync(recipientEmail, subject, htmlContent);
    }

    /**
     * Generic method for sending an HTML email using Gmail SMTP.
     * 
     * @param toEmail   Recipient email
     * @param subject   Subject line
     * @param htmlBody  HTML-formatted body content
     */
    private static void sendEmail(String toEmail, String subject, String htmlBody) {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.starttls.required", "true");
        properties.put("mail.smtp.ssl.protocols", "TLSv1.2");
        properties.put("mail.smtp.connectiontimeout", "15000"); // 15 seconds connection timeout
        properties.put("mail.smtp.timeout", "15000");           // 15 seconds I/O timeout
        properties.put("mail.smtp.writetimeout", "15000");      // 15 seconds write timeout

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(GMAIL_USERNAME, GMAIL_APP_PASSWORD);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(GMAIL_USERNAME, "BPARK System"));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject(subject);
            message.setContent(htmlBody, "text/html; charset=UTF-8");
            Transport.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Test examples (for debugging).
     * Uncomment to send test emails.
     */
    public static void main(String[] args) {
        // Test late pickup email
        // sendLatePickupEmail("user@gmail.com", "George");

        // Test password recovery email
        // sendParkingCodeEmail("user@gmail.com", "George", "pass1234");
    }
}
