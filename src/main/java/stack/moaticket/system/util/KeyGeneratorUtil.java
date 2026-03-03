package stack.moaticket.system.util;

import com.github.f4b6a3.uuid.UuidCreator;

import java.security.SecureRandom;
import java.util.UUID;

public class KeyGeneratorUtil {
    private KeyGeneratorUtil() {};
    private static final char[] CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final int BASE = CHARS.length;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String genRandomizedBase62(int len) {
        StringBuilder sb = new StringBuilder(len);
        while(sb.length() < len) {
            int v = RANDOM.nextInt(256);
            if(v >= 248) continue;
            sb.append(CHARS[v % BASE]);
        }
        return sb.toString();
    }

    public static String genUuidV4() {
        return UUID.randomUUID()
                .toString()
                .replaceAll("-", "");
    }

    public static String genUuidV7() {
        return UuidCreator.getTimeOrderedEpoch()
                .toString()
                .replaceAll("-", "");
    }
}
