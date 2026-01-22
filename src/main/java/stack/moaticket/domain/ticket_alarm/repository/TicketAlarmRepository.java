package stack.moaticket.domain.ticket_alarm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stack.moaticket.domain.ticket_alarm.entity.TicketAlarm;

@Repository
public interface TicketAlarmRepository extends JpaRepository<TicketAlarm, Long>, TicketAlarmQueryDslRepository {
}
