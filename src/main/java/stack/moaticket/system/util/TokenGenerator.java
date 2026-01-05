package stack.moaticket.system.util;


import java.security.SecureRandom;
import java.util.Base64;

public final class TokenGenerator {

    private static final SecureRandom random = new SecureRandom();

    private TokenGenerator() {}

    /**
     * 128-bit random opaque token (Base64 URL-safe, no padding)
     * 길이: 22자
     */
    public static String generateHoldToken() {
        byte[] bytes = new byte[16]; // 128-bit
        random.nextBytes(bytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}