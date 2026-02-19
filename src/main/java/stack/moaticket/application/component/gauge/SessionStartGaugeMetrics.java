package stack.moaticket.application.component.gauge;

public final class SessionStartGaugeMetrics {
    private SessionStartGaugeMetrics() {}

    public static final String SESSION_START_THROUGHPUT = "executor.session-start.throughput";
    public static final String SESSION_START_ERROR = "executor.session-start.error";
    public static final String SESSION_START_PIPELINE = "executor.session-start.pipeline";
}
