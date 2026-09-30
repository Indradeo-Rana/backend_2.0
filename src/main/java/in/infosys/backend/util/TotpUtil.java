package in.infosys.backend.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Locale;

public final class TotpUtil {
    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom RANDOM = new SecureRandom();

    private TotpUtil() {}

    public static String generateSecret() {
        byte[] bytes = new byte[20];
        RANDOM.nextBytes(bytes);
        return encodeBase32(bytes);
    }

    public static boolean verifyCode(String secret, String code) {
        if (secret == null || code == null || !code.matches("\\d{6}")) return false;
        long counter = System.currentTimeMillis() / 1000L / 30L;
        for (long offset = -1; offset <= 1; offset++) {
            if (generateCode(secret, counter + offset).equals(code)) return true;
        }
        return false;
    }

    private static String generateCode(String secret, long counter) {
        try {
            byte[] key = decodeBase32(secret);
            byte[] data = ByteBuffer.allocate(8).putLong(counter).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            return String.format(Locale.ROOT, "%06d", binary % 1_000_000);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate MFA code", e);
        }
    }

    private static String encodeBase32(byte[] data) {
        StringBuilder result = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsLeft = 0;
        for (byte value : data) {
            buffer = (buffer << 8) | (value & 0xff);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                bitsLeft -= 5;
                result.append(BASE32.charAt((buffer >> bitsLeft) & 31));
            }
        }
        if (bitsLeft > 0) result.append(BASE32.charAt((buffer << (5 - bitsLeft)) & 31));
        return result.toString();
    }

    private static byte[] decodeBase32(String value) {
        String normalized = value.replace("=", "").replace(" ", "").toUpperCase(Locale.ROOT);
        ByteBuffer out = ByteBuffer.allocate(normalized.length() * 5 / 8 + 1);
        int buffer = 0;
        int bitsLeft = 0;
        for (char c : normalized.toCharArray()) {
            int index = BASE32.indexOf(c);
            if (index < 0) throw new IllegalArgumentException("Invalid MFA secret");
            buffer = (buffer << 5) | index;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bitsLeft -= 8;
                out.put((byte) ((buffer >> bitsLeft) & 0xff));
            }
        }
        byte[] result = new byte[out.position()];
        out.flip();
        out.get(result);
        return result;
    }
}
