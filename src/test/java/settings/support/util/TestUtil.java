package settings.support.util;

import stack.moaticket.system.util.KeyGeneratorUtil;

import java.util.concurrent.atomic.AtomicLong;

public class TestUtil {
    private TestUtil() {}
    private static final AtomicLong SEQ = new AtomicLong();

    public static String uuid() {
        return KeyGeneratorUtil.genUuidV4();
    }

    public static String uniqueString(String prefix) {
        return prefix + "_" + SEQ.getAndIncrement();
    }

    public static String uniqueEmail() {
        return "test_" + SEQ.getAndIncrement() + "@moa.dev";
    }
}
