package logic;

/**
 * <p>
 * Represents a worker in the parking system. Each worker has a unique ID,
 * a name, and a type indicating their role (e.g., manager or regular worker).
 * </p>
 *
 * <p>
 * This class is used for authentication, permissions, or assigning duties
 * based on worker type.
 * </p>
 * 
 * @author Bahaa
 */
public class Worker {

    /** Unique identifier for the worker */
    private String WorkerID;

    /** Type of the worker (true if manager, false if regular worker) */
    private boolean Type;

    /** Name of the worker */
    private String name;

    /**
     * Constructs a Worker with the given ID, type, and name.
     *
     * @param workerID the unique worker ID
     * @param t        the worker type (true = manager, false = regular)
     * @param name     the name of the worker
     */
    public Worker(String workerID, boolean t, String name) {
        this.WorkerID = workerID;
        this.Type = t;
        this.name = name;
    }

    /** Default constructor */
    public Worker() {
    }

    /**
     * Returns the worker's ID.
     *
     * @return the worker ID
     */
    public String getWorkerID() {
        return WorkerID;
    }

    /**
     * Sets the worker's ID.
     *
     * @param workerID the worker ID to set
     */
    public void setWorkerID(String workerID) {
        this.WorkerID = workerID;
    }

    /**
     * Returns the type of the worker.
     *
     * @return true if manager, false if regular worker
     */
    public boolean getType() {
        return Type;
    }

    /**
     * Returns the name of the worker.
     *
     * @return the worker's name
     */
    public String getname() {
        return name;
    }

    /**
     * Sets the name of the worker.
     *
     * @param name the worker's name to set
     */
    public void setname(String name) {
        this.name = name;
    }

    /**
     * Returns a string representation of the worker.
     *
     * @return a string with the worker's ID
     */
    @Override
    public String toString() {
        return "Worker{" +
                "WorkerID=" + WorkerID +
                '}';
    }
}
