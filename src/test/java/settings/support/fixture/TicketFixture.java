package settings.support.fixture;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import settings.support.util.TestUtil;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.session.entity.Session;
import stack.moaticket.domain.ticket.entity.Ticket;
import stack.moaticket.domain.ticket.repository.TicketRepository;
import stack.moaticket.domain.ticket.type.TicketState;

public class TicketFixture extends BaseFixture<Ticket, Long> {
    private final TicketRepository ticketRepository;

    public TicketFixture(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Override
    protected JpaRepository<Ticket, Long> repo() {
        return ticketRepository;
    }

    @Transactional
    public Ticket create(Member member, Session session, int limit) {
        return save(Ticket.builder()
                .num(TestUtil.generateNumberIncrementally(limit))
                .holdToken(null)
                .expiresAt(null)
                .state(TicketState.AVAILABLE)
                .member(member)
                .session(session)
                .build());
    }
}
