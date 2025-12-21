import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SearchInterface extends Remote {
    /**
     * Start the search on this server.
     * @param targetHash     The MD5 hash to find.
     * @param startRange     The starting index (First character index: 0-94).
     * @param endRange       The ending index (First character index: 0-94).
     * @param numThreads     How many threads this server should use.
     * @param passwordLength The length of the password to find.
     * @return Result string (Password, ID, Time) or null if not found.
     */
    String search(String targetHash, long startRange, long endRange, int numThreads, int passwordLength) throws RemoteException;

    /**
     * Stop all threads on this server immediately.
     */
    void stopSearch() throws RemoteException;
}