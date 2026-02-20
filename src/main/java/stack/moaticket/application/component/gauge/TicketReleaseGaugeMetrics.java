package stack.moaticket.application.component.gauge;

public final class TicketReleaseGaugeMetrics {
    public static final String TICKET_RELEASE_THROUGHPUT = "executor.ticket-release.throughput";
    public static final String TICKET_RELEASE_ERROR = "executor.ticket-release.error";
    public static final String TICKET_RELEASE_PIPELINE = "executor.ticket-release.pipeline";

    public static final String TICKET_RELEASE_REDIS_PRODUCED = "executor.ticket-release.redis.produced";
    public static final String TICKET_RELEASE_REDIS_CONSUMED = "executor.ticket-release.redis.consumed";
    public static final String TICKET_RELEASE_REDIS_REDELIVERED = "executor.ticket-release.redis.redelivered";
    public static final String TICKET_RELEASE_REDIS_ERROR = "executor.ticket-release.redis.error";
    public static final String TICKET_RELEASE_REDIS_PRODUCE_PIPELINE = "executor.ticket-release.redis.produce-pipeline";
    public static final String TICKET_RELEASE_REDIS_CONSUME_PIPELINE = "executor.ticket-release.redis.consume-pipeline";
    public static final String TICKET_RELEASE_REDIS_PEL_PIPELINE = "executor.ticket-release.redis.pel-pipeline";

    public static final String TICKET_RELEASE_REDIS_CONSUMER_LOCKED = "executor.ticket-release.redis.consumer.locked";
    public static final String TICKET_RELEASE_REDIS_CONSUMER_UNLOCKED = "executor.ticket-release.redis.consumer.unlocked";
    public static final String TICKET_RELEASE_REDIS_CONSUMER_ACKED = "executor.ticket-release.redis.consumer.acked";
    public static final String TICKET_RELEASE_REDIS_CONSUMER_DONE = "executor.ticket-release.redis.consumer.done";

    public static final String TICKET_RELEASE_REDIS_PEL_LOCKED = "executor.ticket-release.redis.pel.locked";
    public static final String TICKET_RELEASE_REDIS_PEL_UNLOCKED = "executor.ticket-release.redis.pel.unlocked";
    public static final String TICKET_RELEASE_REDIS_PEL_ACKED = "executor.ticket-release.redis.pel.acked";
    public static final String TICKET_RELEASE_REDIS_PEL_DONE = "executor.ticket-release.redis.pel.done";
}
