import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.atomic.AtomicLong;
import java.util.ArrayList;
import java.util.List;

public class SearchServer extends UnicastRemoteObject implements SearchInterface {

    private final AtomicLong totalChecked = new AtomicLong(0);
    private volatile boolean keepSearching = true;
    private volatile String foundResult = null; 
    private List<BruteForceThread> activeThreads = new ArrayList<>();

    protected SearchServer() throws RemoteException {
        super();
    }

    @Override
    public long getProgress() throws RemoteException {
        return totalChecked.get();
    }

    public void addProgress(long count) {
        totalChecked.addAndGet(count);
    }

    // --- CASE 1: I FOUND IT ---
    public synchronized void foundPassword(String result) {
        this.foundResult = result;
        
        System.out.println("\n#################################################");
        System.out.println(">>> PASSWORD FOUND ON THIS SERVER!");
        System.out.println(">>> Result: " + result);
        System.out.println("#################################################\n");
        
        terminateThreads();
    }

    // --- CASE 2: SOMEONE ELSE FOUND IT ---
    @Override
    public void stopSearch(String winnerInfo) throws RemoteException {
        if (!keepSearching) return; // Already stopped
        
        System.out.println("\n-------------------------------------------------");
        System.out.println(">>> Search Stopped.");
        System.out.println(">>> Password was found at: " + winnerInfo);
        System.out.println("-------------------------------------------------\n");
        
        terminateThreads();
    }

    // Helper to kill threads
    private void terminateThreads() {
        keepSearching = false;
        for (BruteForceThread t : activeThreads) {
            t.stopRunning();
        }
    }

    @Override
    public String search(String targetHash, long start, long end, int numThreads, int length) throws RemoteException {
        System.out.println("Starting search | Length: " + length + " | Range: " + start + "-" + end + " | Threads: " + numThreads);
        
        totalChecked.set(0);
        keepSearching = true;
        foundResult = null;
        activeThreads.clear();

        long totalRange = end - start + 1;
        long chunk = totalRange / numThreads;
        
        for (int i = 0; i < numThreads; i++) {
            long tStart = start + (i * chunk);
            long tEnd = (i == numThreads - 1) ? end : (tStart + chunk - 1); 
            
            if (tStart > end) break;

            BruteForceThread t = new BruteForceThread(i, targetHash, tStart, tEnd, length, this);
            activeThreads.add(t);
            t.start();
        }

        try {
            boolean allFinished = false;
            while (keepSearching && !allFinished) {
                Thread.sleep(100); 
                if (foundResult != null) return foundResult;

                allFinished = true;
                for (BruteForceThread t : activeThreads) {
                    if (t.isAlive()) {
                        allFinished = false;
                        break;
                    }
                }
            }
        } catch (InterruptedException e) { e.printStackTrace(); }

        return foundResult;
    }

    public static void main(String[] args) {
        try {
            try { LocateRegistry.createRegistry(1099); } catch (Exception e) {}
            
            String serverName = "server_1"; 
            if (args.length > 0) serverName = args[0];

            System.setProperty("java.rmi.server.hostname", System.getProperty("java.rmi.server.hostname"));
            Naming.rebind(serverName, new SearchServer());
            
            System.out.println("Server [" + serverName + "] is ready.");
        } catch (Exception e) {
            System.out.println("Server Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}