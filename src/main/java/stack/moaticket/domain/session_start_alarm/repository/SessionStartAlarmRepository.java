package stack.moaticket.domain.session_start_alarm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stack.moaticket.domain.session_start_alarm.entity.SessionStartAlarm;

@Repository
public interface SessionStartAlarmRepository extends JpaRepository<SessionStartAlarm, Long> {
}