package settings.support.util;

import stack.moaticket.system.util.KeyGeneratorUtil;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class TestUtil {
    private TestUtil() {}
    private static final AtomicLong SEQ = new AtomicLong();
    private static final AtomicInteger SEQ_FOR_SEAT_NUM = new AtomicInteger(1);

    public static String uuid() {
        return KeyGeneratorUtil.genUuidV4();
    }

    public static String uniqueString(String prefix) {
        return prefix + "_" + SEQ.getAndIncrement();
    }

    public static String uniqueEmail() {
        return "test_" + SEQ.getAndIncrement() + "@moa.dev";
    }

    public static int generateNumberIncrementally(int limit) {
        int num = SEQ_FOR_SEAT_NUM.getAndIncrement();

        if(num > limit) return -1;
        else return num;
    }
}
