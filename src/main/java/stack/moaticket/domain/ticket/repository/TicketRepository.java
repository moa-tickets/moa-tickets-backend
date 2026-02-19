package stack.moaticket.domain.ticket.repository;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import stack.moaticket.domain.ticket.entity.Ticket;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    @Query(value = """
        SELECT ticket_id
        FROM ticket
        WHERE ticket_state = 'HOLD'
            AND expires_at <= :now
            AND hold_token IS NOT NULL
            AND member_id IS NOT NULL
        LIMIT :batch
        FOR UPDATE SKIP LOCKED;
    """, nativeQuery = true)
    List<Long> getHoldTicketIdList(
            @Param("now") LocalDateTime now,
            @Param("batch") Long batchSize);

    @Modifying
    @Query(value = """
        UPDATE ticket
        SET ticket_state = 'AVAILABLE',
            expires_at = NULL,
            hold_token = NULL,
            member_id = NULL
        WHERE ticket_state = 'HOLD'
            AND expires_at <= :now
            AND hold_token IS NOT NULL
            AND member_id IS NOT NULL
            AND ticket_id IN (:ids)
    """, nativeQuery = true)
    void releaseHoldTickets(
            @Param("now") LocalDateTime now,
            @Param("ids") List<Long> ticketIdList
    );
}
