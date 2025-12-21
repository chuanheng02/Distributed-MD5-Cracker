import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;

public class SearchClient {

    // ASCII range: 32-126 (95 characters)
    private static final int TOTAL_CHARS = 95;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Distributed MD5 Cracker Client (Multi-PC) ===");
        
        // --- 1. User Inputs ---
        System.out.print("Enter MD5 Hash: ");
        String targetHash = scanner.next().trim();
        
        System.out.print("Enter Password Length: ");
        int passwordLength = scanner.nextInt();

        System.out.print("Enter Threads per Server: ");
        int threadsPerServer = scanner.nextInt();

        System.out.print("Enter Number of Servers (1 or 2): ");
        int numServers = scanner.nextInt();

        // --- 2. Connection Setup ---
        List<SearchService> servers = new ArrayList<>();
        List<String> serverIps = new ArrayList<>();

        for (int i = 1; i <= numServers; i++) {
            System.out.println("\n--- Configuring Server " + i + " ---");
            System.out.print("Enter IP Address for Server " + i + " (e.g., 192.168.1.50): ");
            String ip = scanner.next().trim();

            try {
                // Connect to the Registry at the specific IP address
                Registry registry = LocateRegistry.getRegistry(ip, 1099);
                
                // Look up the server by name. 
                // We assume you started Server 1 as "Server1" and Server 2 as "Server2".
                String serverName = "Server" + i;
                SearchService svc = (SearchService) registry.lookup(serverName);
                
                servers.add(svc);
                serverIps.add(ip);
                System.out.println("Success: Connected to " + serverName + " at " + ip);
                
            } catch (Exception e) {
                System.err.println("Error: Failed to connect to Server " + i + " at " + ip);
                System.err.println("Tip: Ensure you started the server with: -Djava.rmi.server.hostname=" + ip);
                e.printStackTrace();
                return; // Stop execution if we can't connect
            }
        }

        System.out.println("\nAll servers connected. Starting distributed search...");

        // --- 3. Partitioning & Execution ---
        ExecutorService clientExecutor = Executors.newFixedThreadPool(numServers);
        CompletionService<String> completionService = new ExecutorCompletionService<>(clientExecutor);

        long startTime = System.currentTimeMillis();
        
        // Calculate the partition size (95 chars / numServers)
        int charsPerServer = TOTAL_CHARS / numServers;
        int remainder = TOTAL_CHARS % numServers;
        int currentStart = 0;

        for (int i = 0; i < numServers; i++) {
            int extra = (i < remainder) ? 1 : 0;
            int currentEnd = currentStart + charsPerServer + extra;

            final int sIdx = currentStart;
            final int eIdx = currentEnd;
            final SearchService server = servers.get(i);
            final String ip = serverIps.get(i);
            final int serverId = i + 1;

            // Submit the task to the client's thread pool
            completionService.submit(() -> {
                System.out.println("-> Dispatching Range [" + sIdx + "-" + eIdx + "] to Server " + serverId + " (" + ip + ")");
                return server.startSearch(targetHash, sIdx, eIdx, threadsPerServer, passwordLength);
            });
            
            currentStart = currentEnd;
        }

        // --- 4. Result Handling ---
        String foundResult = null;
        try {
            // We wait for the first server to return a non-null result (Password Found)
            // Or for all servers to finish returning null (Password Not Found)
            for (int i = 0; i < numServers; i++) {
                Future<String> f = completionService.take(); // Blocks until a server responds
                String result = f.get();
                if (result != null) {
                    foundResult = result;
                    break; // Found the password!
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // --- 5. Cleanup ---
        // Stop all servers immediately (Propagation of "Found" signal)
        System.out.println("\nStopping all servers...");
        for (SearchService s : servers) {
            try { s.stopSearch(); } catch (Exception e) { /* Ignore connection errors during stop */ }
        }
        clientExecutor.shutdownNow();

        // --- 6. Final Report ---
        System.out.println("\n========================================");
        if (foundResult != null) {
            System.out.println("STATUS: PASSWORD FOUND");
            System.out.println(foundResult); // Contains Password, Thread ID
        } else {
            System.out.println("STATUS: PASSWORD NOT FOUND");
            System.out.println("Checked all combinations in the assigned range.");
        }
        System.out.println("Total Search Time: " + duration + " ms");
        System.out.println("========================================");
        
        System.exit(0);
    }
}