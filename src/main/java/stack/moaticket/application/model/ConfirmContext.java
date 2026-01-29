package stack.moaticket.application.model;

public record ConfirmContext(
        Long paymentId,
        Long memberId,
        String orderId,
        String paymentKey,
        long amount,
        boolean alreadyPaid
) {}