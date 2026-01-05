package stack.moaticket.domain.ticket_hold.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import stack.moaticket.domain.ticket_hold.entity.TicketHold;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class TicketHoldCommand {

    private final EntityManager em;

    public void insertAll(List<TicketHold> holds) {
        for (TicketHold hold : holds) {
            em.persist(hold);
        }
        // PK 충돌을 여기서 바로 터뜨리게 flush
        em.flush();
        // clear는 선택(지금은 4건이라 없어도 됨)
        // em.clear();
    }
}
