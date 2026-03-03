package stack.moaticket.domain.session_start_alarm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import stack.moaticket.domain.session_start_alarm.entity.SessionStartAlarm;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SessionStartAlarmRepository extends JpaRepository<SessionStartAlarm, Long>, SessionStartAlarmQueryDslRepository {
    @Query(value = """
        SELECT session_start_alarm_id
        FROM session_start_alarm
        WHERE session_start_alarm_state = 'PENDING'
            AND session_start_alarm_at <= :now
        ORDER BY session_start_alarm_id
        LIMIT :batch
        FOR UPDATE SKIP LOCKED;
    """, nativeQuery = true)
    List<Long> getSessionStartAlarmIdList(
            @Param("now") LocalDateTime now,
            @Param("batch") Long batchSize);

    @Modifying
    @Query(value = """
        UPDATE session_start_alarm
        SET session_start_alarm_state =
            IF(session_start_alarm_at <= :cutoff, 'PASSED', 'PROCESSED'),
            updated_at = NOW(6)
        WHERE session_start_alarm_id IN (:ids)
    """, nativeQuery = true)
    long updateSessionStartAlarm(
            @Param("ids") List<Long> alarmIdList,
            @Param("cutoff") LocalDateTime cutoff);
}