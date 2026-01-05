package stack.moaticket.domain.session_stream.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import stack.moaticket.domain.session_stream.type.SessionStreamState;

import static stack.moaticket.domain.session_stream.entity.QSessionStream.sessionStream;
import static stack.moaticket.domain.session_stream.type.SessionStreamState.READY;

@Repository
@RequiredArgsConstructor
public class SessionStreamRepositoryQueryDsl {
    private final JPAQueryFactory jpaQueryFactory;

    public String getPlaybackIdWhereReadyState(String streamKey) {
        BooleanExpression condition = sessionStream.streamKey.eq(streamKey)
                .and(sessionStream.state.eq(READY));
        return jpaQueryFactory.select(sessionStream.playbackId)
                .from(sessionStream)
                .where(condition)
                .fetchOne();
    }

    public boolean isExistStreamKey(String streamKey, SessionStreamState state) {
        BooleanExpression condition = sessionStream.streamKey.eq(streamKey)
                .and(sessionStream.state.eq(state));

        return jpaQueryFactory.selectOne()
                .from(sessionStream)
                .where(condition)
                .fetchFirst() != null;
    }
}
