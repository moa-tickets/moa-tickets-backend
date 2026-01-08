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
    private static final Set<String> PREFIX_FILTER_SET = Set.of(
            "/swagger-ui",
            "/api-docs",
            "/v3/api-docs"
    );

    public static boolean checkFilter(String uri) {

        if (API_FILTER_SET.contains(uri)){
            return true;
        }

        for (String prefix : PREFIX_FILTER_SET) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
