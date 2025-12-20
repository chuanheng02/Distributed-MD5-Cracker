import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SearchServer implements SearchService {

    private final String serverName;
    private final AtomicBoolean shouldStop = new AtomicBoolean(false);
    private ExecutorService executor;
    private PrintWriter fileLogger;

    public SearchServer(String name) {
        this.serverName = name;
        try {
            // Initialize logging to server_x.log
            fileLogger = new PrintWriter(new FileWriter(name + ".log", true), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String logEntry = String.format("[%s] %s: %s", timestamp, serverName, message);
        System.out.println(logEntry); // Console
        if (fileLogger != null) {
            fileLogger.println(logEntry); // File
        }
    }

    @Override
    public String startSearch(String targetHash, int startCharIdx, int endCharIdx, int numThreads, int passwordLength) throws RemoteException {
        shouldStop.set(false);
        log("Starting search. Threads: " + numThreads + ", Range indices: " + startCharIdx + "-" + endCharIdx);

        executor = Executors.newFixedThreadPool(numThreads);
        ExecutorCompletionService<String> completionService = new ExecutorCompletionService<>(executor);

        // Calculate sub-ranges for local threads
        int totalCharsInRange = endCharIdx - startCharIdx;
        int charsPerThread = totalCharsInRange / numThreads;
        int remainder = totalCharsInRange % numThreads;
        
        int currentStart = startCharIdx;

        for (int i = 0; i < numThreads; i++) {
            int extra = (i < remainder) ? 1 : 0;
            int currentEnd = currentStart + charsPerThread + extra;
            
            // Create and submit worker
            // We pass 'shouldStop' so the worker can check it and terminate early
            CrackerWorker worker = new CrackerWorker(i, currentStart, currentEnd, targetHash, passwordLength, shouldStop);
            completionService.submit(worker);
            
            log("Thread " + i + " assigned range index: " + currentStart + " to " + currentEnd);
            currentStart = currentEnd;
        }

        try {
            // Wait for first result or all to finish
            for (int i = 0; i < numThreads; i++) {
                Future<String> resultFuture = completionService.poll(100, TimeUnit.MILLISECONDS);
                
                // If we get a result, it means someone found it
                if (resultFuture != null && resultFuture.get() != null) {
                    String foundMsg = resultFuture.get();
                    log("Password found by local thread: " + foundMsg);
                    stopSearch(); // Kill other local threads
                    return foundMsg;
                }
                
                // Check if we were told to stop by the client
                if (shouldStop.get()) {
                    break;
                }
                
                // If no result yet, decrement i to keep waiting, but don't loop indefinitely
                // (Simplified logic: actually we just wait for the pool)
                if (resultFuture == null) i--; 
            }
        } catch (Exception e) {
            log("Error during search: " + e.getMessage());
        } finally {
            stopSearch(); // Ensure cleanup
        }
        
        log("Search completed (Not found or stopped).");
        return null;
    }

    @Override
    public void stopSearch() throws RemoteException {
        if (!shouldStop.get()) {
            shouldStop.set(true);
            log("Received STOP signal. Shutting down threads.");
            if (executor != null) {
                executor.shutdownNow();
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java SearchServer <server_name> <port>");
            System.err.println("Example: java SearchServer Server1 1099");
            return;
        }
        
        String name = args[0];
        int port = (args.length > 1) ? Integer.parseInt(args[1]) : 1099;

        try {
            SearchServer server = new SearchServer(name);
            SearchService stub = (SearchService) UnicastRemoteObject.exportObject(server, 0);

            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(port);
            } catch (Exception e) {
                registry = LocateRegistry.getRegistry(port);
            }
            
            registry.rebind(name, stub);
            System.out.println(name + " bound in registry on port " + port);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}