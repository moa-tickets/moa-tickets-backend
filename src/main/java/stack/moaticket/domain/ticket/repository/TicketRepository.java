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

    // 잠긴 ROW 제외 Lock 걸기
    @Query(value = """
    SELECT *
    FROM ticket
    WHERE session_id = :sessionId
      AND ticket_id IN (:ids)
      AND ticket_state = 'AVAILABLE'
    ORDER BY ticket_id
    FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    List<Ticket> findForUpdateSkipLocked(@Param("sessionId") Long sessionId,
                                         @Param("ids") List<Long> ids);

    // booking availbale -> hold 원자적 업데이트
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE ticket
           SET ticket_state = 'HOLD',
               member_id    = :memberId,
               hold_token   = :holdToken,
               expires_at   = :expiresAt
         WHERE session_id   = :sessionId
           AND ticket_id IN (:ticketIds)
           AND ticket_state = 'AVAILABLE'
        """, nativeQuery = true)
    int holdAtomicAvailableOnly(
            @Param("sessionId") Long sessionId,
            @Param("memberId") Long memberId,
            @Param("holdToken") String holdToken,
            @Param("expiresAt") LocalDateTime expiresAt,
            @Param("ticketIds") List<Long> ticketIds
    );
}
