import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

public class CrackerWorker implements Callable<String> {

    private final int workerId;
    private final int startIndex;
    private final int endIndexExclusive;
    private final byte[] targetBytes;
    private final int length;
    private final AtomicBoolean shouldStop;
    
    // ASCII range 32-126
    private static final int FIRST = 32;
    private static final char[] CHARSET;
    static {
        CHARSET = new char[95];
        for (int i = 0; i < 95; i++) CHARSET[i] = (char) (FIRST + i);
    }

    public CrackerWorker(int workerId, int startIndex, int endIndexExclusive, String targetHash, int length, AtomicBoolean shouldStop) {
        this.workerId = workerId;
        this.startIndex = startIndex; // This is the index in CHARSET (0-94)
        this.endIndexExclusive = endIndexExclusive;
        this.length = length;
        this.shouldStop = shouldStop;
        this.targetBytes = hexStringToByteArray(targetHash);
    }

    @Override
    public String call() throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] candidateBytes = new byte[length];

        // This worker only handles the FIRST character based on its assigned range
        // Then it iterates all combinations for the remaining positions
        
        for (int i = startIndex; i < endIndexExclusive; i++) {
            if (shouldStop.get()) return null;
            
            candidateBytes[0] = (byte) CHARSET[i];
            
            // Recursive generation for the rest of the positions
            String result = generate(md, candidateBytes, 1);
            if (result != null) return result;
        }
        return null;
    }

    // Recursive helper
    private String generate(MessageDigest md, byte[] candidateBytes, int position) {
        if (shouldStop.get()) return null;

        if (position == length) {
            // Check Hash
            md.reset();
            md.update(candidateBytes);
            if (Arrays.equals(md.digest(), targetBytes)) {
                return new String(candidateBytes) + " (Thread ID: " + workerId + ")";
            }
            return null;
        }

        // Iterate through all 95 chars for this position
        for (char c : CHARSET) {
            candidateBytes[position] = (byte) c;
            String res = generate(md, candidateBytes, position + 1);
            if (res != null) return res;
            if (shouldStop.get()) return null;
        }
        return null;
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}