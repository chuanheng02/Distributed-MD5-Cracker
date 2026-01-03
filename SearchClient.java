import java.rmi.Naming;
import java.util.Scanner;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SearchClient {
    private static final int TOTAL_PARTITIONS = 95; 
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static volatile boolean finished = false;

    // --- HELPER: ROBUST INPUT READER ---
    private static int readInt(Scanner scanner, String prompt, int min, int max) {
        int input;
        while (true) {
            System.out.print(prompt);
            if (scanner.hasNextInt()) {
                input = scanner.nextInt();
                scanner.nextLine(); // Consume newline
                if (input >= min && input <= max) {
                    return input;
                } else {
                    System.out.println(">> Error: Please enter a number between " + min + " and " + max + ".");
                }
            } else {
                System.out.println(">> Error: Invalid input. Please enter a numeric value.");
                scanner.next(); 
            }
        }
    }
    // -----------------------------------

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            // --- IPS ---
            String SERVER_1_IP = "10.87.159.58"; 
            String SERVER_2_IP = "10.87.146.145"; 
            // -----------

            System.out.println("--- Distributed Password Cracker Client ---");
            
            // 1. Get Hash
            System.out.print("Enter MD5 Hash: ");
            String rawHash = scanner.nextLine().trim();
            while (rawHash.isEmpty()) { 
                System.out.print(">> Error: Hash cannot be empty. Enter MD5 Hash: ");
                rawHash = scanner.nextLine().trim();
            }

            // 2. Validate Password Length
            int rawPwdLen = readInt(scanner, "Enter Password Length: ", 2, 6);

            // 3. Validate Threads per Server
            int rawThreads = readInt(scanner, "Enter Threads per Server: ", 1, 10);

            // 4. Validate Number of Servers
            int numServers = readInt(scanner, "Enter Number of Servers: ", 1, 2);

            // --- FINAL COPIES FOR THREAD SAFETY ---
            final String hash = rawHash;
            final int pwdLen = rawPwdLen;
            final int threads = rawThreads;
            // --------------------------------------

            System.out.println("Connecting to Server 1...");
            SearchInterface server1 = (SearchInterface) Naming.lookup("rmi://" + SERVER_1_IP + "/server_1");
            
            SearchInterface server2 = null;
            if (numServers == 2) {
                System.out.println("Connecting to Server 2...");
                server2 = (SearchInterface) Naming.lookup("rmi://" + SERVER_2_IP + "/server_2");
            }

            long rangePerServer = TOTAL_PARTITIONS / numServers;
            long s1Start = 0;
            long s1End = (numServers == 1) ? (TOTAL_PARTITIONS - 1) : (rangePerServer - 1);
            long s2Start = rangePerServer;
            long s2End = TOTAL_PARTITIONS - 1;

            double totalCombinations = Math.pow(95, pwdLen);
            System.out.println("Total Combinations: " + String.format("%,.0f", totalCombinations));

            System.out.println("\n--- Starting Search ---");
            LocalDateTime startDateTime = LocalDateTime.now();
            long startTimeMillis = System.currentTimeMillis();
            System.out.println("Start Time: " + dtf.format(startDateTime));
            System.out.println("--------------------------------------------------------------------------------");

            final String[] result = {null};
            final int[] winningServer = {0}; 

            // Thread 1
            Thread t1 = new Thread(() -> {
                try {
                    String res = server1.search(hash, s1Start, s1End, threads, pwdLen);
                    if (res != null) { result[0] = res; winningServer[0] = 1; finished = true; }
                } catch (Exception e) {
                    // --- NEW ERROR HANDLING ---
                    System.out.println("\n>> [CRITICAL ERROR] Server 1 failed or disconnected: " + e.getMessage());
                }
            });
            t1.start();

            // Thread 2
            Thread t2 = null;
            if (numServers == 2) {
                final SearchInterface s2Ref = server2;
                t2 = new Thread(() -> {
                    try {
                        String res = s2Ref.search(hash, s2Start, s2End, threads, pwdLen);
                        if (res != null) { result[0] = res; winningServer[0] = 2; finished = true; }
                    } catch (Exception e) {
                        // --- NEW ERROR HANDLING ---
                        System.out.println("\n>> [CRITICAL ERROR] Server 2 failed or disconnected: " + e.getMessage());
                    }
                });
                t2.start();
            }

            // Monitor Loop
            while (!finished) {
                try {
                    Thread.sleep(1000); 
                    
                    long count1 = 0;
                    try { count1 = server1.getProgress(); } catch(Exception e){}
                    
                    long count2 = 0;
                    if (server2 != null) {
                        try { count2 = server2.getProgress(); } catch(Exception e){}
                    }

                    long totalTried = count1 + count2;
                    long currentTime = System.currentTimeMillis();
                    long timeElapsed = (currentTime - startTimeMillis) / 1000; 

                    if (timeElapsed > 0) {
                        double percentage = (totalTried / totalCombinations) * 100.0;
                        long rate = totalTried / timeElapsed; 
                        long remaining = (long)totalCombinations - totalTried;
                        long etaSeconds = (rate > 0) ? (remaining / rate) : 0;
                        
                        String etaString = String.format("%02d:%02d:%02d", etaSeconds / 3600, (etaSeconds % 3600) / 60, etaSeconds % 60);
                        System.out.print(String.format("\r[%.4f%%] Tried: %,d (Rate: %,d/s) (ETA: %s)   ", percentage, totalTried, rate, etaString));
                    }
                    
                    // Stop if threads are dead/finished
                    if (!t1.isAlive() && (t2 == null || !t2.isAlive())) finished = true;

                } catch (InterruptedException e) { break; }
            }

            // --- REPORTING & STOPPING ---
            System.out.println("\n--------------------------------------------------------------------------------");
            long endTimeMillis = System.currentTimeMillis();
            
            if (result[0] != null) {
                // STOP THE LOSERS
                if (winningServer[0] == 1) {
                     if (server2 != null) try { server2.stopSearch("Server 1"); } catch (Exception e) {}
                } else if (winningServer[0] == 2) {
                     try { server1.stopSearch("Server 2"); } catch (Exception e) {}
                }

                System.out.println("SUCCESS! Password Found:");
                System.out.println("Result          : " + result[0]);
                System.out.println("Found At Server : Server " + winningServer[0]);
            } else {
                System.out.println("Search finished. Password not found.");
            }
            
            System.out.println("Total Duration  : " + (endTimeMillis - startTimeMillis) + " ms");

        } catch (Exception e) {
            System.out.println("Client Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}