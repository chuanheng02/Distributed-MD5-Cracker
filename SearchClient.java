import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;

public class SearchClient {
    // 95 printable ASCII characters (32-126)
    private static final int TOTAL_CHARS = 95;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // --- 1. User Inputs ---
        System.out.println("=== Distributed MD5 Cracker Client ===");
        System.out.print("Enter MD5 Hash: ");
        String targetHash = scanner.next();
        
        System.out.print("Enter Password Length: ");
        int passwordLength = scanner.nextInt();

        System.out.print("Enter Number of Servers (1 or 2): ");
        int numServers = scanner.nextInt();

        System.out.print("Enter Threads per Server: ");
        int threadsPerServer = scanner.nextInt();

        // --- 2. RMI Connection ---
        List<SearchService> servers = new ArrayList<>();
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099); // Change "localhost" if using VMs
            
            servers.add((SearchService) registry.lookup("Server1"));
            if (numServers > 1) {
                servers.add((SearchService) registry.lookup("Server2"));
            }
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
            return;
        }

        // --- 3. Partitioning & Execution ---
        ExecutorService clientExecutor = Executors.newFixedThreadPool(numServers);
        CompletionService<String> completionService = new ExecutorCompletionService<>(clientExecutor);

        long startTime = System.currentTimeMillis();
        
        // Static Partitioning: Split 0-95 based on server count
        int charsPerServer = TOTAL_CHARS / numServers;
        int remainder = TOTAL_CHARS % numServers;
        int currentStart = 0;

        for (int i = 0; i < numServers; i++) {
            int extra = (i < remainder) ? 1 : 0;
            int currentEnd = currentStart + charsPerServer + extra;

            final int sIdx = currentStart;
            final int eIdx = currentEnd;
            final SearchService server = servers.get(i);

            // Submit search task asynchronously
            completionService.submit(() -> {
                return server.startSearch(targetHash, sIdx, eIdx, threadsPerServer, passwordLength);
            });
            
            System.out.println("Dispatched to Server " + (i+1) + ": Range " + sIdx + "-" + eIdx);
            currentStart = currentEnd;
        }

        // --- 4. Result Handling ---
        try {
            String foundResult = null;
            // Wait for the first server to return a non-null result (found)
            // or for all to finish (not found)
            for (int i = 0; i < numServers; i++) {
                Future<String> f = completionService.take(); // Blocks until one finishes
                String result = f.get();
                if (result != null) {
                    foundResult = result;
                    break; // Found it!
                }
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // Stop all servers immediately
            for (SearchService s : servers) {
                try { s.stopSearch(); } catch (Exception e) { /* Ignore connection errors on stop */ }
            }

            // --- 5. Report ---
            System.out.println("\n==============================");
            if (foundResult != null) {
                System.out.println("PASSWORD FOUND!");
                System.out.println(foundResult); // Server returns formatted string
            } else {
                System.out.println("Password not found in search space.");
            }
            System.out.println("Total Search Time: " + duration + " ms");
            System.out.println("==============================");

            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}