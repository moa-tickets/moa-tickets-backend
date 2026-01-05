package stack.moaticket.domain.ticket_hold.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import stack.moaticket.domain.ticket_hold.entity.TicketHold;

public interface TicketHoldRepository extends JpaRepository<TicketHold, Long> {
}
