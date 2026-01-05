package stack.moaticket.domain.session_stream.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stack.moaticket.domain.session.entity.Session;
import stack.moaticket.domain.session_stream.entity.SessionStream;
import stack.moaticket.domain.session_stream.repository.SessionStreamRepositoryQueryDsl;
import stack.moaticket.domain.session_stream.type.SessionStreamState;
import stack.moaticket.system.util.DateUtil;
import stack.moaticket.system.util.KeyGeneratorUtil;

@Service
@RequiredArgsConstructor
public class SessionStreamService {
    private final SessionStreamRepositoryQueryDsl sessionStreamRepositoryQueryDsl;

    public SessionStream create(Session session) {
        return SessionStream.builder()
                .session(session)
                .streamKey(KeyGeneratorUtil.genUuidV7())
                .playbackId(genPlaybackId())
                .state(SessionStreamState.READY)
                .build();
    }

    public String validateAndGetPlaybackId(String streamKey) {
        return sessionStreamRepositoryQueryDsl.getPlaybackIdWhereReadyState(streamKey);
    }

    public boolean isExistLiveSession(String streamKey) {
        return sessionStreamRepositoryQueryDsl.isExistStreamKey(streamKey, SessionStreamState.LIVE);
    }

    private String genPlaybackId() {
        return DateUtil.genDateString() + "-" + KeyGeneratorUtil.genRandomizedBase62(12);
    }
}
