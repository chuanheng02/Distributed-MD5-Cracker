import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SearchInterface extends Remote {
    String search(String hash, long start, long end, int numThreads, int length) throws RemoteException;
    
    long getProgress() throws RemoteException;
    
    // CHANGED: Now accepts the name of the winner!
    void stopSearch(String winnerInfo) throws RemoteException;
}