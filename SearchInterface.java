import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SearchInterface extends Remote {
    // Main search method
    String search(String hash, long start, long end, int numThreads, int length) throws RemoteException;
    
    // NEW: Method for Client to ask "How many have you checked?"
    long getProgress() throws RemoteException;
    
    // Method to stop search if another server finds it
    void stopSearch() throws RemoteException;
}