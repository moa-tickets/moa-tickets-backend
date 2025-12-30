package stack.moaticket.domain.payment_ticket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stack.moaticket.domain.payment_ticket.entity.PaymentTicket;

@Repository
public interface PaymentTicketRepository extends JpaRepository<PaymentTicket, Long> {
}
