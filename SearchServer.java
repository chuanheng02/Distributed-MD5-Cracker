import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicLong;

public class SearchServer extends UnicastRemoteObject implements SearchInterface {

    // Counter for the Live ETA
    private final AtomicLong totalChecked = new AtomicLong(0);
    
    // Flag to stop threads if password is found
    private volatile boolean keepSearching = true;

    protected SearchServer() throws RemoteException {
        super();
    }

    // --- RMI Interface Methods ---

    @Override
    public long getProgress() throws RemoteException {
        return totalChecked.get();
    }

    @Override
    public void stopSearch() throws RemoteException {
        keepSearching = false;
    }

    @Override
    public String search(String targetHash, long start, long end, int numThreads, int length) throws RemoteException {
        System.out.println("Starting search | Length: " + length + " | Range: " + start + " - " + end);
        
        // Reset counter and flag for new search
        totalChecked.set(0);
        keepSearching = true;

        // --- BRUTE FORCE LOGIC ---
        // We assume 'start' and 'end' refer to the ASCII value of the FIRST character.
        
        for (long firstChar = start; firstChar <= end; firstChar++) {
            if (!keepSearching) return null;

            // Prepare the prefix (first character)
            char c = (char) firstChar;
            StringBuilder sb = new StringBuilder();
            sb.append(c);

            // If length is 1, check immediately
            if (length == 1) {
                totalChecked.incrementAndGet();
                if (md5(sb.toString()).equals(targetHash)) return sb.toString();
            } else {
                // If length > 1, go deeper
                String result = recursiveCheck(sb, length - 1, targetHash);
                if (result != null) return result;
            }
        }
        
        return null;
    }

    // --- Helper Methods ---

    // Recursive helper to generate remaining characters
    private String recursiveCheck(StringBuilder current, int charsLeft, String targetHash) {
        if (!keepSearching) return null;

        if (charsLeft == 0) {
            // Full password built. Check it!
            String candidate = current.toString();
            
            // --- UPDATE COUNTER FOR ETA ---
            // We update every check. 
            // (For extreme speed optimization, you could update every 1000 checks, but this is safer for now)
            totalChecked.incrementAndGet(); 
            // ------------------------------

            if (md5(candidate).equals(targetHash)) {
                return candidate;
            }
            return null;
        }

        // Loop through all printable ASCII (32-126)
        for (int i = 32; i <= 126; i++) {
            if (!keepSearching) return null;
            
            current.append((char) i);
            String res = recursiveCheck(current, charsLeft - 1, targetHash);
            if (res != null) return res;
            current.setLength(current.length() - 1); // Backtrack
        }
        return null;
    }

    // MD5 Hasher
    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : messageDigest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // --- Main Method (Updated) ---

    public static void main(String[] args) {
        try {
            // 1. Start Registry (if not already running)
            try { LocateRegistry.createRegistry(1099); } catch (Exception e) {}
            
            // 2. Determine Server Name
            String serverName = "server_1"; // Default
            
            if (args.length > 0) {
                serverName = args[0]; // Use the name you typed!
            }

            // 3. Bind to Registry
            System.setProperty("java.rmi.server.hostname", System.getProperty("java.rmi.server.hostname"));
            Naming.rebind(serverName, new SearchServer());
            
            System.out.println("Server [" + serverName + "] is ready.");
            
        } catch (Exception e) {
            System.out.println("Server Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}