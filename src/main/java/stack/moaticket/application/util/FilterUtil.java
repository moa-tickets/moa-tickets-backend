package stack.moaticket.application.util;

import java.util.HashSet;
import java.util.Set;

public class FilterUtil {
    private FilterUtil() {};
    private static final Set<String> API_FILTER_SET = Set.of(
            "/login",
            "/rtmp/on-publish",
            "/rtmp/on-done",
            "/favicon.ico",
            "/error",
            "/swagger-ui.html",
            "/api-docs",
            "/health"
    );

    public static boolean checkFilter(String uri) {
        return API_FILTER_SET.contains(uri);
    }
}
