package stack.moaticket.application.model;

import stack.moaticket.system.redis.model.StreamKey;

public class TicketReleaseConsumerKey implements StreamKey<TicketReleaseConsumerValue> {
    private static final String KEY = "ticket-release";

    @Override
    public String get() {
        return KEY;
    }

    @Override
    public Class<TicketReleaseConsumerValue> type() {
        return TicketReleaseConsumerValue.class;
    }
}
