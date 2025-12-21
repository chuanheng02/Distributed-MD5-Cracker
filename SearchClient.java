import java.rmi.Naming;
import java.util.Scanner;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SearchClient {
    // Total partitions (based on ASCII 32-126 = 95 characters)
    private static final int TOTAL_PARTITIONS = 95; 
    
    // Formatter for timestamps
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // Flag to stop the ETA loop when found
    private static volatile boolean finished = false;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            // --- CONFIGURATION ---
            // UPDATE THESE IPS TO MATCH YOUR REAL SETUP
            String SERVER_1_IP = "172.20.10.2"; 
            String SERVER_2_IP = "172.20.10.3"; 
            // ---------------------

            System.out.println("--- Distributed Password Cracker Client ---");
            System.out.print("Enter MD5 Hash: ");
            String hash = scanner.nextLine().trim();
            
            System.out.print("Enter Password Length: ");
            int pwdLen = scanner.nextInt();
            
            System.out.print("Enter Threads per Server: ");
            int threads = scanner.nextInt();
            
            System.out.print("Enter Number of Servers (1 or 2): ");
            int numServers = scanner.nextInt();

            // 1. Connect
            System.out.println("Connecting to Server 1...");
            SearchInterface server1 = (SearchInterface) Naming.lookup("rmi://" + SERVER_1_IP + "/server_1");
            
            SearchInterface server2 = null;
            if (numServers == 2) {
                System.out.println("Connecting to Server 2...");
                server2 = (SearchInterface) Naming.lookup("rmi://" + SERVER_2_IP + "/server_2");
            }

            // 2. Setup Partitions
            long rangePerServer = TOTAL_PARTITIONS / numServers;
            
            long s1Start = 0; // Server 1 does the first half
            long s1End = (numServers == 1) ? (TOTAL_PARTITIONS - 1) : (rangePerServer - 1);
            
            long s2Start = rangePerServer; // Server 2 does the second half
            long s2End = TOTAL_PARTITIONS - 1;

            // 3. Calc Total for ETA
            // Total = 95 ^ length
            double totalCombinations = Math.pow(95, pwdLen);
            System.out.println("Total Combinations: " + String.format("%,.0f", totalCombinations));

            System.out.println("\n--- Starting Search ---");
            
            LocalDateTime startDateTime = LocalDateTime.now();
            long startTimeMillis = System.currentTimeMillis();
            System.out.println("Start Time: " + dtf.format(startDateTime));
            System.out.println("--------------------------------------------------------------------------------");

            // 4. Start Search Threads
            final String[] result = {null};
            final int[] winningServer = {0}; // 0=None, 1=Server1, 2=Server2

            // Thread for Server 1
            Thread t1 = new Thread(() -> {
                try {
                    String res = server1.search(hash, s1Start, s1End, threads, pwdLen);
                    if (res != null) { 
                        result[0] = res; 
                        winningServer[0] = 1; 
                        finished = true; 
                    }
                } catch (Exception e) {}
            });
            t1.start();

            // Thread for Server 2
            Thread t2 = null;
            if (numServers == 2) {
                final SearchInterface s2Ref = server2;
                t2 = new Thread(() -> {
                    try {
                        String res = s2Ref.search(hash, s2Start, s2End, threads, pwdLen);
                        if (res != null) { 
                            result[0] = res; 
                            winningServer[0] = 2; 
                            finished = true; 
                        }
                    } catch (Exception e) {}
                });
                t2.start();
            }

            // 5. MONITORING LOOP (The Live ETA)
            while (!finished) {
                try {
                    Thread.sleep(1000); // Check every 1 second
                    
                    // Fetch progress from servers
                    long count1 = 0;
                    try { count1 = server1.getProgress(); } catch(Exception e){}
                    
                    long count2 = 0;
                    if (server2 != null) {
                        try { count2 = server2.getProgress(); } catch(Exception e){}
                    }

                    long totalTried = count1 + count2;
                    long currentTime = System.currentTimeMillis();
                    long timeElapsed = (currentTime - startTimeMillis) / 1000; // seconds

                    if (timeElapsed > 0) {
                        double percentage = (totalTried / totalCombinations) * 100.0;
                        long rate = totalTried / timeElapsed; // attempts/sec
                        
                        // Calc ETA
                        long remaining = (long)totalCombinations - totalTried;
                        long etaSeconds = (rate > 0) ? (remaining / rate) : 0;
                        
                        String etaString = String.format("%02d:%02d:%02d", 
                            etaSeconds / 3600, (etaSeconds % 3600) / 60, etaSeconds % 60);

                        // Overwrite line
                        System.out.print(String.format("\r[%.4f%%] Tried: %,d (Rate: %,d/s) (ETA: %s)   ", 
                            percentage, totalTried, rate, etaString));
                    }
                    
                    // Stop if threads died naturally (search exhausted)
                    if (!t1.isAlive() && (t2 == null || !t2.isAlive())) {
                        finished = true;
                    }

                } catch (InterruptedException e) { break; }
            }

            // 6. Report Results
            System.out.println("\n--------------------------------------------------------------------------------");
            long endTimeMillis = System.currentTimeMillis();
            LocalDateTime endDateTime = LocalDateTime.now();

            // Stop servers
            try { server1.stopSearch(); } catch (Exception e) {}
            if (server2 != null) try { server2.stopSearch(); } catch (Exception e) {}

            if (result[0] != null) {
                System.out.println("SUCCESS! Password Found:");
                System.out.println("Result          : " + result[0]);
                System.out.println("Found At Server : Server " + winningServer[0]);
                System.out.println("Start Time      : " + dtf.format(startDateTime));
                System.out.println("End Time        : " + dtf.format(endDateTime));
                System.out.println("Total Duration  : " + (endTimeMillis - startTimeMillis) + " ms");
            } else {
                System.out.println("Search finished. Password not found.");
                System.out.println("End Time        : " + dtf.format(endDateTime));
            }

        } catch (Exception e) {
            System.out.println("Client Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}