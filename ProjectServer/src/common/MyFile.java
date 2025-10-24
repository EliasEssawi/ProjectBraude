package common;

import java.io.Serializable;

/**
 * <p>
 * Represents a file to be transferred between client and server.
 * This class implements {@link Serializable} to allow the file's data
 * and associated command to be sent over a network.
 * </p>
 *
 * <p>
 * It contains the file content in bytes and a controller command string
 * to help route the file to the appropriate handler.
 * </p>
 * 
 * @author Bahaa
 */
public class MyFile implements Serializable {

    private static final long serialVersionUID = 1L;

    /** A string indicating the controller or command related to this file */
    private String controllerCommand;

    /** The raw file data in byte array format */
    private byte[] fileBytes;

    /**
     * Constructs a new {@code MyFile} with a controller command and file content.
     *
     * @param controllerCommand a string representing the controller or action
     * @param fileBytes the contents of the file as a byte array
     */
    public MyFile(String controllerCommand, byte[] fileBytes) {
        this.controllerCommand = controllerCommand;
        this.fileBytes = fileBytes;
    }

    /**
     * Returns the controller command associated with this file.
     *
     * @return the controller command
     */
    public String getControllerCommand() {
        return controllerCommand;
    }

    /**
     * Returns the file's content as a byte array.
     *
     * @return the file bytes
     */
    public byte[] getFileBytes() {
        return fileBytes;
    }
}
