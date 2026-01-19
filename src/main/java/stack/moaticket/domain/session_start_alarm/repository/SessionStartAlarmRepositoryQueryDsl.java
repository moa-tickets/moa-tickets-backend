package stack.moaticket.domain.session_start_alarm.repository;

import com.querydsl.core.types.dsl.*;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import stack.moaticket.domain.session_start_alarm.entity.SessionStartAlarm;
import stack.moaticket.domain.session_start_alarm.type.SessionStartAlarmState;

import java.time.LocalDateTime;
import java.util.List;

import static stack.moaticket.domain.session_start_alarm.entity.QSessionStartAlarm.sessionStartAlarm;

@Repository
@RequiredArgsConstructor
public class SessionStartAlarmRepositoryQueryDsl {
    private final JPAQueryFactory jpaQueryFactory;

    public List<SessionStartAlarm> getSessionStartAlarmList(Long batchSize, SessionStartAlarmState state, LocalDateTime now) {
        BooleanExpression condition = sessionStartAlarm.state.eq(state)
                .and(sessionStartAlarm.alarmAt.loe(now));

        return jpaQueryFactory.select(sessionStartAlarm)
                .from(sessionStartAlarm)
                .where(condition)
                .limit(batchSize)
                .orderBy(sessionStartAlarm.alarmAt.asc())
                .fetch();
    }

    public List<SessionStartAlarm> getClaimedSessionStartAlarmList(LocalDateTime now) {
        BooleanExpression condition = sessionStartAlarm.state.eq(SessionStartAlarmState.CLAIMED)
                .and(sessionStartAlarm.alarmAt.loe(now));

        return jpaQueryFactory.selectDistinct(sessionStartAlarm)
                .from(sessionStartAlarm)
                .join(sessionStartAlarm.session).fetchJoin()
                .join(sessionStartAlarm.session.concert).fetchJoin()
                .where(condition)
                .orderBy(sessionStartAlarm.alarmAt.asc())
                .fetch();
    }

    public void updatePendingSessionStartAlarmToSkipped(List<SessionStartAlarm> candidateList, LocalDateTime now) {
        LocalDateTime cutoff = now.minusMinutes(5);
        BooleanExpression condition = sessionStartAlarm.in(candidateList)
                .and(sessionStartAlarm.state.eq(SessionStartAlarmState.PENDING))
                .and(sessionStartAlarm.alarmAt.loe(cutoff));

        jpaQueryFactory.update(sessionStartAlarm)
                .set(sessionStartAlarm.state, SessionStartAlarmState.SKIPPED)
                .where(condition)
                .execute();
    }

    public void updatePendingSessionStartAlarmToClaimed(List<SessionStartAlarm> candidateList, LocalDateTime now) {
        LocalDateTime cutoff = now.minusMinutes(5);

        BooleanExpression condition = sessionStartAlarm.in(candidateList)
                .and(sessionStartAlarm.state.eq(SessionStartAlarmState.PENDING))
                .and(sessionStartAlarm.alarmAt.loe(now))
                .and(sessionStartAlarm.alarmAt.gt(cutoff));

        jpaQueryFactory.update(sessionStartAlarm)
                .set(sessionStartAlarm.state, SessionStartAlarmState.CLAIMED)
                .set(sessionStartAlarm.claimedAt, now)
                .where(condition)
                .execute();
    }

    public void updateClaimedSessionStartAlarmToSent(List<SessionStartAlarm> succeededList) {
        BooleanExpression condition = sessionStartAlarm.in(succeededList)
                        .and(sessionStartAlarm.state.eq(SessionStartAlarmState.CLAIMED));

        jpaQueryFactory.update(sessionStartAlarm)
                .set(sessionStartAlarm.state, SessionStartAlarmState.SENT)
                .where(condition)
                .execute();
    }

    public void updateClaimedSessionStartAlarmToCleaned(LocalDateTime now) {
        BooleanExpression condition = sessionStartAlarm.state.eq(SessionStartAlarmState.CLAIMED)
                .and(sessionStartAlarm.claimedAt.isNotNull())
                .and(sessionStartAlarm.claimedAt.loe(now.minusSeconds(10)));

        jpaQueryFactory.update(sessionStartAlarm)
                .set(sessionStartAlarm.state, SessionStartAlarmState.CLEANED)
                .where(condition)
                .execute();
    }

    public void updateClaimedSessionStartAlarmToDisconnected(List<SessionStartAlarm> failedList) {
        BooleanExpression condition = sessionStartAlarm.in(failedList)
                .and(sessionStartAlarm.state.eq(SessionStartAlarmState.CLAIMED));

        jpaQueryFactory.update(sessionStartAlarm)
                .set(sessionStartAlarm.state, SessionStartAlarmState.DISCONNECTED)
                .where(condition)
                .execute();
    }
}
