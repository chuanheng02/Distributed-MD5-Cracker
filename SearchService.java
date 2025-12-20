import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SearchService extends Remote {
    // Starts the search in the specified range.
    // Returns the password details if found, or null if not found in this range.
    String startSearch(String targetHash, int startCharIdx, int endCharIdx, int numThreads, int passwordLength) throws RemoteException;
    
    // Command to stop search immediately (called when another server finds the password).
    void stopSearch() throws RemoteException;
}