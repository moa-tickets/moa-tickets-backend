package stack.moaticket.system.util;

import java.time.LocalDate;

public class DateUtil {
    private DateUtil() {};

    public static String genDateString() {
        LocalDate now = LocalDate.now();
        return now.toString().replaceAll("-", "");
    }
}
