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
            "/health"
    );
    private static final Set<String> PREFIX_FILTER_SET = Set.of(
            "/swagger-ui",
            "/api-docs",
            "/v3/api-docs"
    );

    public static boolean checkFilter(String uri) {
        return API_FILTER_SET.contains(uri) || PREFIX_FILTER_SET.stream().anyMatch(uri::startsWith);
    }
}
