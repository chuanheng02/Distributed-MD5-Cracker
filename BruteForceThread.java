import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.nio.charset.StandardCharsets;

public class BruteForceThread extends Thread {
    
    // ASCII 32-126
    private static final int FIRST = 32;
    private static final int LAST = 126;
    private static final int CHARSET_SIZE = LAST - FIRST + 1; // 95
    private static final char[] CHARSET;

    static {
        CHARSET = new char[CHARSET_SIZE];
        for (int i = 0; i < CHARSET_SIZE; i++) {
            CHARSET[i] = (char) (FIRST + i);
        }
    }

    private int threadID;
    private long startCharIndex; 
    private long endCharIndex;   
    private int passwordLength;
    private byte[] targetBytes;
    private SearchServer server; 
    private volatile boolean keepRunning = true;

    // --- SPEED CONFIGURATION ---
    // Update progress only once every 1,000,000 checks
    private long localCount = 0; 
    private static final long BATCH_SIZE = 1000000; 
    // ---------------------------

    // ThreadLocal MD5 (Reuse MD5 instance per thread)
    private static final ThreadLocal<MessageDigest> THREAD_LOCAL_MD5 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 unavailable", e);
        }
    });

    public BruteForceThread(int id, String targetHash, long startRange, long endRange, int length, SearchServer server) {
        this.threadID = id;
        this.startCharIndex = startRange;
        this.endCharIndex = endRange;
        this.passwordLength = length;
        this.server = server;
        this.targetBytes = hexStringToByteArray(targetHash);
    }

    @Override
    public void run() {
        MessageDigest md = THREAD_LOCAL_MD5.get();
        byte[] candidateBytes = new byte[passwordLength];
        long startTime = System.currentTimeMillis();

        // 1. Iterate through Assigned First Characters (Prefix)
        for (long firstCharIdx = startCharIndex; firstCharIdx <= endCharIndex && keepRunning; firstCharIdx++) {
            
            if (firstCharIdx >= CHARSET_SIZE) break;

            candidateBytes[0] = (byte) CHARSET[(int)firstCharIdx];

            // 2. Recursive Search for the rest
            if (passwordLength > 1) {
                recursiveSearch(1, candidateBytes, md, startTime);
            } else {
                check(md, candidateBytes, startTime);
            }
        }
        
        // Final flush of progress count (in case it ended with < 1,000,000 checks pending)
        if (localCount > 0) {
            server.addProgress(localCount);
        }
    }

    // Recursive helper to fill remaining bytes
    private void recursiveSearch(int index, byte[] candidateBytes, MessageDigest md, long startTime) {
        if (!keepRunning) return;

        // Base Case: Array is full. Hash it.
        if (index == passwordLength) {
            check(md, candidateBytes, startTime);
            return;
        }

        // Recursive Step
        for (int i = 0; i < CHARSET_SIZE; i++) {
            candidateBytes[index] = (byte) CHARSET[i];
            recursiveSearch(index + 1, candidateBytes, md, startTime);
            
            if (!keepRunning) return;
        }
    }

    private void check(MessageDigest md, byte[] candidateBytes, long startTime) {
        md.update(candidateBytes);
        
        if (Arrays.equals(md.digest(), targetBytes)) {
            long duration = System.currentTimeMillis() - startTime;
            String password = new String(candidateBytes, StandardCharsets.UTF_8);
            String res = String.format("Password: %s, ThreadID: %d, Time: %dms", password, this.threadID, duration);
            
            // Notify Server
            server.foundPassword(res);
        }

        // --- BATCHED UPDATE ---
        localCount++;
        if (localCount >= BATCH_SIZE) {
            server.addProgress(localCount);
            localCount = 0;
        }
    }

    public void stopRunning() {
        this.keepRunning = false;
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}