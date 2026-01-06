package stack.moaticket.system.util;


import java.security.SecureRandom;
import java.util.Base64;

public final class TokenGenerator {

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final String HOLD_TOKEN_PREFIX = "hold_";

    private TokenGenerator() {}

    // prefix + 128-bit random opaque token
    // Base64 URL-safe, no padding
    public static String generateToken(String prefix) {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);

        String encoded = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        return prefix + encoded;
    }

    public static String generateHoldToken() {
        return generateToken("hold_");
    }

    public static String generateOrderId() {
        return generateToken("order_");
    }

}