package stack.moaticket.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.application.dto.ConcertDto;
import stack.moaticket.domain.concert.entity.Concert;
import stack.moaticket.domain.concert.service.ConcertService;
import stack.moaticket.domain.session.entity.Session;
import stack.moaticket.domain.session.service.SessionService;
import stack.moaticket.domain.ticket.service.TicketService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ConcertService concertService;
    private final SessionService sessionService;
    private final TicketService ticketService;

    @Transactional
    public long createConcert(ConcertDto.ConcertRequest request){
        Concert concert = concertService.insertConcert(request);
        List<Session> sessions = sessionService.insertSessions(concert.getId(), request.getSessions());
        for (Session session : sessions){
            ticketService.insertTickets(session);
        }
        return concert.getId();
    }
    public ConcertDto.ConcertDetailResponse getConcertDetail(long concertId){
        Concert concert = concertService.getConcertById(concertId);
        List<Session> sessions = sessionService.getSessionsByConcertId(concertId);
        ConcertDto.ConcertDetailResponse response = concertService.getConcertDetail(concert, sessions);
        return response;
    }
}
