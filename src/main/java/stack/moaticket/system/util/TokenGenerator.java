package stack.moaticket.system.util;


import java.security.SecureRandom;
import java.util.Base64;

public final class TokenGenerator {

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final String HOLD_TOKEN_PREFIX = "hold_";

    private TokenGenerator() {}

    // hold 토큰 생성 (27자)
    // 128-bit random opaque token (Base64 URL-safe, no padding)
    public static String generateHoldToken() {
        return generate(secureRandom);
    }

    static String generate(java.util.Random random) {
        byte[] bytes = new byte[16]; // 128-bit
        random.nextBytes(bytes);

        String encoded = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        return HOLD_TOKEN_PREFIX + encoded;
    }

}