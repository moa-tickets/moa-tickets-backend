package settings.support.fixture;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.domain.payment.entity.Payment;
import stack.moaticket.domain.payment_ticket.entity.PaymentTicket;
import stack.moaticket.domain.payment_ticket.repository.PaymentTicketRepository;
import stack.moaticket.domain.ticket.entity.Ticket;

public class PaymentTicketFixture extends BaseFixture<PaymentTicket, Long> {
    private final PaymentTicketRepository paymentTicketRepository;

    public PaymentTicketFixture(PaymentTicketRepository paymentTicketRepository) {
        this.paymentTicketRepository = paymentTicketRepository;
    }

    @Override
    protected JpaRepository<PaymentTicket, Long> repo() {
        return paymentTicketRepository;
    }

    @Transactional
    public PaymentTicket create(Ticket ticket, Payment payment) {
        return save(PaymentTicket.builder()
                .ticket(ticket)
                .payment(payment)
                .build());
    }
}
