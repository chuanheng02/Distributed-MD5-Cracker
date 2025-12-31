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
    
    // --- Track Start Time for Rate Calculation ---
    private volatile long searchStartTime = 0;
    // ---------------------------------------------

    public static String logFileName = "server_log.txt"; 

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    protected SearchServer() throws RemoteException {
        super();
    }

    private void logToFile(String message) {
        try (FileWriter fw = new FileWriter(logFileName, true);
             PrintWriter pw = new PrintWriter(fw)) {
            String timestamp = LocalDateTime.now().format(dtf);
            pw.println("[" + timestamp + "] " + message);
        } catch (IOException e) {
            System.out.println(">> [ERROR] Could not write to " + logFileName);
        }
    }

    private static void logException(String prefix, Exception e) {
        try (FileWriter fw = new FileWriter(logFileName, true);
             PrintWriter pw = new PrintWriter(fw)) {
            String timestamp = LocalDateTime.now().format(dtf);
            pw.println("[" + timestamp + "] [EXCEPTION] " + prefix + ": " + e.toString());
        } catch (IOException io) {}
    }

    // --- HELPER: Calculate Rate ---
    private String getRateStats() {
        long endTime = System.currentTimeMillis();
        long duration = endTime - searchStartTime;
        long total = totalChecked.get();
        
        long rate = 0;
        if (duration > 0) {
            rate = (total * 1000) / duration; // Convert ms to seconds
        }
        
        return String.format("Total Checked: %,d | Duration: %d ms | Rate: %,d attempts/sec", total, duration, rate);
    }
    // ------------------------------

    @Override
    public long getProgress() throws RemoteException {
        return totalChecked.get();
    }

    public void addProgress(long count) {
        totalChecked.addAndGet(count);
    }

    public synchronized void foundPassword(String result) {
        this.foundResult = result;
        
        String stats = getRateStats();
        
        // --- SPLIT INTO TWO LINES FOR SCREENSHOT ---
        String line1 = ">>> SUCCESS: Password found! Result details: " + result;
        String line2 = ">>> Search Stats: " + stats;
        
        System.out.println("\n" + line1);
        System.out.println(line2 + "\n");
        
        logToFile(line1); 
        logToFile(line2); 
        // -------------------------------------------
        
        terminateThreads();
    }

    @Override
    public void stopSearch(String winnerInfo) throws RemoteException {
        if (!keepSearching) return; 
        
        String stats = getRateStats();
        
        // --- SPLIT INTO TWO LINES FOR SCREENSHOT ---
        String line1 = ">>> STOP: Search halted. Password found remotely at: " + winnerInfo;
        String line2 = ">>> Search Stats: " + stats;
        
        System.out.println("\n" + line1);
        System.out.println(line2 + "\n");
        
        logToFile(line1); 
        logToFile(line2); 
        // -------------------------------------------
        
        terminateThreads();
    }

    private void terminateThreads() {
        keepSearching = false;
        logToFile("Thread Stop Signal Sent. Stopping " + activeThreads.size() + " active threads.");
        for (BruteForceThread t : activeThreads) {
            t.stopRunning();
        }
    }

    @Override
    public String search(String targetHash, long start, long end, int numThreads, int length) throws RemoteException {
        String startMsg = String.format("Request Received. Starting new search. Length: %d | Total Range: %d-%d | Threads to Create: %d", length, start, end, numThreads);
        System.out.println(startMsg);
        logToFile("---------------------------------------------------------------");
        logToFile(startMsg);
        
        // --- Reset Stats ---
        totalChecked.set(0);
        searchStartTime = System.currentTimeMillis(); 
        // -------------------

        keepSearching = true;
        foundResult = null;
        activeThreads.clear();

        long totalRange = end - start + 1;
        long chunk = totalRange / numThreads;
        
        try {
            for (int i = 0; i < numThreads; i++) {
                long tStart = start + (i * chunk);
                long tEnd = (i == numThreads - 1) ? end : (tStart + chunk - 1); 
                
                if (tStart > end) break;

                BruteForceThread t = new BruteForceThread(i, targetHash, tStart, tEnd, length, this);
                activeThreads.add(t);
                
                String threadLog = String.format("Thread-%d Created. Assigned Range: %d-%d. Status: STARTED.", i, tStart, tEnd);
                logToFile(threadLog);
                
                t.start();
            }
        } catch (Exception e) {
            logException("Error during thread creation", e);
            throw new RemoteException("Thread creation failed", e);
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
        } catch (InterruptedException e) { 
            logException("Server Interrupted", e);
            e.printStackTrace(); 
        }

        return foundResult;
    }

    public static void main(String[] args) {
        try {
            try { LocateRegistry.createRegistry(1099); } catch (Exception e) {}
            
            String serverName = "server_1"; 
            if (args.length > 0) serverName = args[0];

            logFileName = serverName + "_log.txt";

            System.setProperty("java.rmi.server.hostname", System.getProperty("java.rmi.server.hostname"));
            Naming.rebind(serverName, new SearchServer());
            
            String msg = "Server [" + serverName + "] is ready.";
            System.out.println(msg);
            
            try (FileWriter fw = new FileWriter(logFileName, true);
                 PrintWriter pw = new PrintWriter(fw)) {
                DateTimeFormatter dtfStart = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                pw.println("===============================================================");
                pw.println("[" + LocalDateTime.now().format(dtfStart) + "] SERVER STARTUP: " + msg);
                pw.println("===============================================================");
            } catch (Exception e) {}

        } catch (Exception e) {
            System.out.println("Server Error: " + e.getMessage());
            e.printStackTrace();
            logException("Main Server Crash", e);
        }
    }
}