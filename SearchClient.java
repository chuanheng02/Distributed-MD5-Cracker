import java.rmi.Naming;
import java.util.Scanner;
import java.time.LocalDateTime; // For Date/Time
import java.time.format.DateTimeFormatter; // For Formatting

public class SearchClient {
    // Partitioning based on First Character (ASCII 32-126 is roughly 95 chars)
    private static final int TOTAL_PARTITIONS = 95; 

    // Date Formatter (e.g., "2025-12-21 10:30:00")
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Track which server found the password
    private static volatile int winningServer = 0;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            // --- USER CONFIGURATION ---
            String SERVER_1_IP = "172.20.10.2"; 
            String SERVER_2_IP = "172.20.10.3"; 
            // --------------------------

            System.out.println("--- Distributed Password Cracker Client ---");
            System.out.print("Enter MD5 Hash: ");
            String hash = scanner.nextLine().trim(); // Added .trim() to fix "space" bugs
            
            System.out.print("Enter Password Length: ");
            int pwdLen = scanner.nextInt();
            
            System.out.print("Enter Threads per Server: ");
            int threads = scanner.nextInt();
            
            System.out.print("Enter Number of Servers (1 or 2): ");
            int numServers = scanner.nextInt();

            // 1. Connect to Servers
            System.out.println("Connecting to Server 1 at " + SERVER_1_IP + "...");
            SearchInterface server1 = (SearchInterface) Naming.lookup("rmi://" + SERVER_1_IP + "/server_1");
            
            SearchInterface server2 = null;
            if (numServers == 2) {
                System.out.println("Connecting to Server 2 at " + SERVER_2_IP + "...");
                server2 = (SearchInterface) Naming.lookup("rmi://" + SERVER_2_IP + "/server_2");
            }

            // 2. Partition the Work
            long rangePerServer = TOTAL_PARTITIONS / numServers;
            
            // Server 1 Range
            long s1Start = 0;
            long s1End = (numServers == 1) ? (TOTAL_PARTITIONS - 1) : (rangePerServer - 1);
            
            // Server 2 Range
            long s2Start = rangePerServer;
            long s2End = TOTAL_PARTITIONS - 1;

            System.out.println("\n--- Starting Distributed Search ---");
            
            // Capture Start Time
            LocalDateTime startDateTime = LocalDateTime.now();
            long startTimeMillis = System.currentTimeMillis();
            System.out.println("Start Time: " + dtf.format(startDateTime));

            // 3. Launch Threads to call Servers asynchronously
            final String[] result = {null};
            
            // Thread for Server 1
            Thread t1 = new Thread(() -> {
                try {
                    String res = server1.search(hash, s1Start, s1End, threads, pwdLen);
                    if (res != null) {
                        result[0] = res;
                        winningServer = 1; // Mark Server 1 as the winner
                    }
                } catch (Exception e) { e.printStackTrace(); }
            });

            // Thread for Server 2
            Thread t2 = null;
            if (numServers == 2) {
                final SearchInterface s2Ref = server2;
                t2 = new Thread(() -> {
                    try {
                        String res = s2Ref.search(hash, s2Start, s2End, threads, pwdLen);
                        if (res != null) {
                            result[0] = res;
                            winningServer = 2; // Mark Server 2 as the winner
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                });
            }

            t1.start();
            if (t2 != null) t2.start();

            // 4. Wait for Completion & "Fake" Progress Monitor
            boolean s1Done = false;
            boolean s2Done = (numServers == 1);

            while (result[0] == null) {
                if (!t1.isAlive()) s1Done = true;
                if (t2 != null && !t2.isAlive()) s2Done = true;
                
                if (s1Done && s2Done) break;
                
                // NOTE: Real ETA requires server feedback. 
                // We just sleep here to wait for the result.
                Thread.sleep(100); 
            }

            // Capture End Time
            long endTimeMillis = System.currentTimeMillis();
            LocalDateTime endDateTime = LocalDateTime.now();

            // 5. Stop & Report
            if (result[0] != null) {
                // Stop other servers immediately
                try { server1.stopSearch(); } catch (Exception e) {}
                if (server2 != null) try { server2.stopSearch(); } catch (Exception e) {}

                System.out.println("\n============================================");
                System.out.println("           PASSWORD FOUND!                  ");
                System.out.println("============================================");
                
                // Parse the result string if it looks like "Password: xyz, ThreadID: 5..."
                // Or just print it directly if parsing is risky
                System.out.println("Raw Result      : " + result[0]);
                
                System.out.println("--------------------------------------------");
                System.out.println("Found At Server : Server " + winningServer);
                System.out.println("Start Time      : " + dtf.format(startDateTime));
                System.out.println("End Time        : " + dtf.format(endDateTime));
                System.out.println("Total Duration  : " + (endTimeMillis - startTimeMillis) + " ms");
                System.out.println("============================================");

            } else {
                System.out.println("\nSearch finished. Password not found.");
                System.out.println("End Time: " + dtf.format(endDateTime));
            }

        } catch (Exception e) {
            System.out.println("Client Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}