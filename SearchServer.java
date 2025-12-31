import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.atomic.AtomicLong;
import java.util.ArrayList;
import java.util.List;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SearchServer extends UnicastRemoteObject implements SearchInterface {

    private final AtomicLong totalChecked = new AtomicLong(0);
    private volatile boolean keepSearching = true;
    private volatile String foundResult = null; 
    private List<BruteForceThread> activeThreads = new ArrayList<>();
    
    // --- DYNAMIC LOG FILENAME ---
    // Default is server_log.txt, but main() will change this to server_1_log.txt, etc.
    public static String logFileName = "server_log.txt"; 
    // ----------------------------

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    protected SearchServer() throws RemoteException {
        super();
    }

    // Updated Helper: Uses the dynamic 'logFileName'
    private void logToFile(String message) {
        try (FileWriter fw = new FileWriter(logFileName, true);
             PrintWriter pw = new PrintWriter(fw)) {
            
            String timestamp = LocalDateTime.now().format(dtf);
            pw.println("[" + timestamp + "] " + message);
            
        } catch (IOException e) {
            System.out.println(">> [ERROR] Could not write to " + logFileName);
        }
    }

    @Override
    public long getProgress() throws RemoteException {
        return totalChecked.get();
    }

    public void addProgress(long count) {
        totalChecked.addAndGet(count);
    }

    public synchronized void foundPassword(String result) {
        this.foundResult = result;
        String msg = ">>> PASSWORD FOUND ON THIS SERVER! Result: " + result;
        System.out.println("\n" + msg + "\n");
        logToFile(msg); 
        terminateThreads();
    }

    @Override
    public void stopSearch(String winnerInfo) throws RemoteException {
        if (!keepSearching) return; 
        String msg = ">>> Search Stopped. Password found at: " + winnerInfo;
        System.out.println("\n" + msg + "\n");
        logToFile(msg); 
        terminateThreads();
    }

    private void terminateThreads() {
        keepSearching = false;
        for (BruteForceThread t : activeThreads) {
            t.stopRunning();
        }
    }

    @Override
    public String search(String targetHash, long start, long end, int numThreads, int length) throws RemoteException {
        String startMsg = String.format("Starting search | Length: %d | Range: %d-%d | Threads: %d", length, start, end, numThreads);
        System.out.println(startMsg);
        logToFile(startMsg);
        
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

            // --- SET DYNAMIC LOG NAME ---
            // This updates the static variable so the log matches the server name
            logFileName = serverName + "_log.txt";
            // ----------------------------

            System.setProperty("java.rmi.server.hostname", System.getProperty("java.rmi.server.hostname"));
            Naming.rebind(serverName, new SearchServer());
            
            String msg = "Server [" + serverName + "] is ready.";
            System.out.println(msg);
            
            // Log startup
            try (FileWriter fw = new FileWriter(logFileName, true);
                 PrintWriter pw = new PrintWriter(fw)) {
                DateTimeFormatter dtfStart = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                pw.println("--------------------------------------------------");
                pw.println("[" + LocalDateTime.now().format(dtfStart) + "] " + msg);
            } catch (Exception e) {}

        } catch (Exception e) {
            System.out.println("Server Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}