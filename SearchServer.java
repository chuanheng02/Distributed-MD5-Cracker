import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class SearchServer extends UnicastRemoteObject implements SearchInterface {
    private String serverName;
    private List<BruteForceThread> threads = new ArrayList<>();
    private volatile boolean keepRunning = true;

    protected SearchServer(String name) throws RemoteException {
        super();
        this.serverName = name;
    }

    @Override
    public String search(String targetHash, long startRange, long endRange, int numThreads, int passwordLength) throws RemoteException {
        this.keepRunning = true;
        this.threads.clear();
        
        // LOGGING: Required by Assignment
        log("Server started. Assigned First-Char Range: " + startRange + " to " + endRange + ", Threads: " + numThreads);

        // Partitioning: Split the assigned range among local threads
        long totalRange = endRange - startRange + 1;
        long chunk = totalRange / numThreads;
        long remainder = totalRange % numThreads;
        
        long currentStart = startRange;

        // Create and start threads
        for (int i = 0; i < numThreads; i++) {
            long extra = (i < remainder) ? 1 : 0;
            long tEnd = currentStart + chunk + extra - 1;
            
            if (tEnd >= currentStart) { // Only start thread if it has work
                BruteForceThread t = new BruteForceThread(i, targetHash, currentStart, tEnd, passwordLength, this);
                threads.add(t);
                t.start();
                log("Thread " + i + " started. Range: " + currentStart + " to " + tEnd);
            }
            currentStart = tEnd + 1;
        }

        // Wait for threads to finish
        try {
            for (BruteForceThread t : threads) {
                t.join();
            }
        } catch (InterruptedException e) {
            log("Server interrupted.");
        }

        // Check results
        for (BruteForceThread t : threads) {
            if (t.getResult() != null) {
                log("Password found by Thread " + t.getThreadID());
                return t.getResult(); // Returns "Password, ID, Time"
            }
        }
        
        return null; // Not found by this server
    }

    @Override
    public void stopSearch() throws RemoteException {
        this.keepRunning = false;
        for (BruteForceThread t : threads) {
            t.stopRunning();
        }
        log("Stop command received. All threads stopped.");
    }

    // Helper: Write to server_x.log
    public synchronized void log(String message) {
        try (PrintWriter out = new PrintWriter(new FileWriter(serverName + ".log", true))) {
            out.println(new java.util.Date() + " : " + message);
        } catch (IOException e) {
            System.err.println("Error writing to log: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java -Djava.rmi.server.hostname=<THIS_PC_IP> SearchServer <server_name>");
            System.out.println("Example: java -Djava.rmi.server.hostname=192.168.0.11 SearchServer server_1");
            return;
        }
        try {
            // Start RMI Registry on port 1099
            try { 
                java.rmi.registry.LocateRegistry.createRegistry(1099); 
                System.out.println("RMI Registry started on port 1099.");
            } catch (Exception e) {
                System.out.println("RMI Registry already running.");
            }

            String name = args[0];
            SearchServer server = new SearchServer(name);
            Naming.rebind(name, server);
            System.out.println(name + " is ready and waiting for the Client...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}