import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

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
    private String result = null; 

    // ThreadLocal MD5 for performance
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
        int suffixLen = passwordLength - 1;

        // Iterate through Assigned First Characters
        for (long firstCharIdx = startCharIndex; firstCharIdx <= endCharIndex && keepRunning; firstCharIdx++) {
            
            if (firstCharIdx >= CHARSET_SIZE) break;

            candidateBytes[0] = (byte) CHARSET[(int)firstCharIdx];

            // Case: Length 1 (Unlikely but handled)
            if (suffixLen == 0) {
                 check(md, candidateBytes, startTime);
                 continue;
            }

            // Odometer Logic for Suffix
            int[] indices = new int[suffixLen]; 
            boolean finished = false;

            while (!finished && keepRunning) {
                for (int i = 0; i < suffixLen; i++) {
                    candidateBytes[i + 1] = (byte) CHARSET[indices[i]];
                }

                check(md, candidateBytes, startTime);

                // Increment odometer
                for (int pos = suffixLen - 1; pos >= 0; pos--) {
                    indices[pos]++;
                    if (indices[pos] >= CHARSET_SIZE) {
                        indices[pos] = 0;
                        if (pos == 0) finished = true;
                    } else {
                        break;
                    }
                }
            }
        }
    }

    private void check(MessageDigest md, byte[] candidateBytes, long startTime) {
        md.update(candidateBytes);
        byte[] digest = md.digest();

        if (Arrays.equals(digest, targetBytes)) {
            long duration = System.currentTimeMillis() - startTime;
            String password = new String(candidateBytes);
            // Format required by Assignment: "Password, Thread ID, Time"
            this.result = String.format("Password: %s, ThreadID: %d, Time: %dms", password, this.threadID, duration);
            this.keepRunning = false; 
        }
    }

    public void stopRunning() {
        this.keepRunning = false;
    }

    public String getResult() {
        return result;
    }

    public int getThreadID() {
        return threadID;
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