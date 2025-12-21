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
    
    // Keep track of active threads to stop them later
    private List<BruteForceThread> activeThreads = new ArrayList<>();

    protected SearchServer() throws RemoteException {
        super();
    }

    @Override
    public long getProgress() throws RemoteException {
        return totalChecked.get();
    }

    // Called by BruteForceThread to update ETA efficiently
    public void addProgress(long count) {
        totalChecked.addAndGet(count);
    }

    // --- UPDATED METHOD: PRINTS WHEN FOUND ---
    public synchronized void foundPassword(String result) {
        this.foundResult = result;
        
        // Print to the Server Console so you can see it immediately
        System.out.println("\n========================================");
        System.out.println(">>> PASSWORD FOUND: " + result);
        System.out.println("========================================\n");
        
        stopSearch(); // Stop everyone else
    }
    // -----------------------------------------

    @Override
    public void stopSearch() {
        keepSearching = false;
        // Stop all worker threads
        for (BruteForceThread t : activeThreads) {
            t.stopRunning();
        }
    }

    @Override
    public String search(String targetHash, long start, long end, int numThreads, int length) throws RemoteException {
        System.out.println("Starting search | Length: " + length + " | Range: " + start + "-" + end + " | Threads: " + numThreads);
        
        // Reset state
        totalChecked.set(0);
        keepSearching = true;
        foundResult = null;
        activeThreads.clear();

        // --- PARTITION WORKLOAD AMONG THREADS ---
        long totalRange = end - start + 1;
        long chunk = totalRange / numThreads;
        
        // Launch Threads
        for (int i = 0; i < numThreads; i++) {
            long tStart = start + (i * chunk);
            long tEnd = (i == numThreads - 1) ? end : (tStart + chunk - 1); // Ensure last thread gets remainder
            
            if (tStart > end) break; // Safety check

            BruteForceThread t = new BruteForceThread(i, targetHash, tStart, tEnd, length, this);
            activeThreads.add(t);
            t.start();
        }

        // --- WAIT FOR RESULT ---
        try {
            boolean allFinished = false;
            while (keepSearching && !allFinished) {
                Thread.sleep(100); // Check every 100ms
                
                if (foundResult != null) return foundResult;

                // Check if all threads died naturally (search finished, not found)
                allFinished = true;
                for (BruteForceThread t : activeThreads) {
                    if (t.isAlive()) {
                        allFinished = false;
                        break;
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return foundResult; // Returns null if not found
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