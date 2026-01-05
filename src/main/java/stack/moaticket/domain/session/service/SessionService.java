package stack.moaticket.domain.session.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stack.moaticket.application.dto.SessionDto;
import stack.moaticket.domain.concert.entity.Concert;
import stack.moaticket.domain.concert.repository.ConcertRepositoryQueryDsl;
import stack.moaticket.domain.session.entity.Session;
import stack.moaticket.domain.session.repository.SessionRepository;
import stack.moaticket.domain.session.repository.SessionRepositoryQueryDsl;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionService {
    private final ConcertRepositoryQueryDsl concertRepositoryQueryDsl;
    private final SessionRepository sessionRepository;
    private final SessionRepositoryQueryDsl sessionRepositoryQueryDsl;

    public List<Session> insertSessions(long concertId, List<SessionDto.SessionRequest> requests){
        Concert concert = concertRepositoryQueryDsl.getConcert(concertId);

        List<Session> sessions = requests.stream()
                .map(request -> request.toEntity(concert))
                .toList();

        sessionRepository.saveAll(sessions);
        return sessions;
    }
    public List<Session> getSessionsByConcertId(long concertId){

        return sessionRepositoryQueryDsl.getSessions(concertId);
    }

    public Session getSession(long sessionId){

        return sessionRepositoryQueryDsl.getSessionById(sessionId);
    }
}
