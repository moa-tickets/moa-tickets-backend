package stack.moaticket.system.alarm.sse.component.gauge;

public final class SseGaugeMetrics {
    private SseGaugeMetrics() {}

    public static final String SSE_EMITTER_TOTAL = "alarm.sse.emitter.total";
    public static final String SSE_MEMBER_TOTAL = "alarm.sse.member.total";

    public static final String SSE_SEND_ATTEMPT_COUNTER = "alarm.sse.send.attempt";
    public static final String SSE_SEND_FAILURE_COUNTER = "alarm.sse.send.fail";
    public static final String SSE_SEND_SECONDS = "alarm.sse.send.seconds";

    public static final String SSE_EXECUTOR_ACTIVE = "alarm.sse.executor.active";
    public static final String SSE_EXECUTOR_QUEUE_SIZE = "alarm.sse.executor.queue.size";
    public static final String SSE_EXECUTOR_POOL_SIZE = "alarm.sse.executor.pool.size";
}
