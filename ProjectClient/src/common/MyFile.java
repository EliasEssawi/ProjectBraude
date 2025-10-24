package common;

import java.io.Serializable;

/**
 * The {@code MyFile} class is a simple data container used to transfer file content
 * and associated controller command information between the server and the client.
 *
 * <p>This class implements {@link Serializable} so it can be sent through
 * object streams in a client-server architecture (e.g., using OCSF).
 * 
 * <p>It is commonly used to send image or report data from the server to specific
 * controllers on the client side (e.g., ManagerFrameController).
 * 
 * @author Bahaa
 */
public class MyFile implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * A string command indicating which controller should handle this file.
     * Example: "ManagerFrameController SHOW_PARKING_REPORT"
     */
    private String controllerCommand;

    /**
     * The byte array containing the file's binary content (e.g., image data).
     */
    private byte[] fileBytes;

    /**
     * Constructs a new {@code MyFile} object with a specific controller command
     * and file content.
     *
     * @param controllerCommand The identifier used to dispatch this file to a controller.
     * @param fileBytes The raw binary content of the file.
     */
    public MyFile(String controllerCommand, byte[] fileBytes) {
        this.controllerCommand = controllerCommand;
        this.fileBytes = fileBytes;
    }

    /**
     * Returns the controller command associated with this file.
     *
     * @return The controller command string.
     */
    public String getControllerCommand() {
        return controllerCommand;
    }

    /**
     * Returns the byte content of the file.
     *
     * @return The file as a byte array.
     */
    public byte[] getFileBytes() {
        return fileBytes;
    }
}
