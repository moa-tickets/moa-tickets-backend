package stack.moaticket.domain.session_start_alarm.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.*;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import stack.moaticket.domain.session_start_alarm.dto.SessionStartAlarmMetaDto;
import stack.moaticket.domain.session_start_alarm.type.SessionStartAlarmState;

import java.time.LocalDateTime;
import java.util.List;

import static stack.moaticket.domain.session_start_alarm.entity.QSessionStartAlarm.sessionStartAlarm;

@Repository
@RequiredArgsConstructor
public class SessionStartAlarmQueryDslRepositoryImpl implements SessionStartAlarmQueryDslRepository {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<Long> getSessionStartAlarmIdList(LocalDateTime now, Long batchSize, SessionStartAlarmState state) {
        BooleanExpression condition = sessionStartAlarm.state.eq(state)
                .and(sessionStartAlarm.alarmAt.loe(now));

        return jpaQueryFactory.select(sessionStartAlarm.id)
                .from(sessionStartAlarm)
                .where(condition)
                .limit(batchSize)
                .fetch();
    }

    @Override
    public void updatePendingSessionStartAlarmToPassed(LocalDateTime now, List<Long> alarmIdList) {
        LocalDateTime cutoff = now.minusMinutes(5);

        BooleanExpression condition = sessionStartAlarm.id.in(alarmIdList)
                .and(sessionStartAlarm.state.eq(SessionStartAlarmState.PENDING))
                .and(sessionStartAlarm.alarmAt.loe(cutoff));

        jpaQueryFactory.update(sessionStartAlarm)
                .set(sessionStartAlarm.state, SessionStartAlarmState.PASSED)
                .where(condition)
                .execute();
    }

    @Override
    public void updatePendingSessionStartAlarmToProcessed(LocalDateTime now, List<Long> alarmIdList) {
        LocalDateTime cutoff = now.minusMinutes(5);

        BooleanExpression condition = sessionStartAlarm.id.in(alarmIdList)
                .and(sessionStartAlarm.state.eq(SessionStartAlarmState.PENDING))
                .and(sessionStartAlarm.alarmAt.gt(cutoff))
                .and(sessionStartAlarm.alarmAt.loe(now));

        jpaQueryFactory.update(sessionStartAlarm)
                .set(sessionStartAlarm.state, SessionStartAlarmState.PROCESSED)
                .where(condition)
                .execute();
    }

    @Override
    public List<SessionStartAlarmMetaDto> getProcessedSessionStartAlarmList(List<Long> alarmIdList) {
        BooleanExpression condition = sessionStartAlarm.id.in(alarmIdList)
                .and(sessionStartAlarm.state.eq(SessionStartAlarmState.PROCESSED));

        return jpaQueryFactory.select(
                        Projections.constructor(
                                SessionStartAlarmMetaDto.class,
                                sessionStartAlarm.id,
                                sessionStartAlarm.member.id,
                                sessionStartAlarm.session.id,
                                sessionStartAlarm.session.concert.name,
                                sessionStartAlarm.type,
                                sessionStartAlarm.session.date
                        )
                )
                .from(sessionStartAlarm)
                .where(condition)
                .orderBy(sessionStartAlarm.alarmAt.asc())
                .fetch();
    }
}
