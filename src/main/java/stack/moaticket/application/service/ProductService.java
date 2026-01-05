package stack.moaticket.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.application.dto.ConcertDetailDto;
import stack.moaticket.application.dto.ConcertListDto;
import stack.moaticket.application.dto.CreateConcertDto;
import stack.moaticket.application.dto.SessionDto;
import stack.moaticket.domain.concert.entity.Concert;
import stack.moaticket.domain.concert.service.ConcertService;
import stack.moaticket.domain.hall.entity.Hall;
import stack.moaticket.domain.hall.service.HallService;
import stack.moaticket.domain.session.entity.Session;
import stack.moaticket.domain.session.service.SessionService;
import stack.moaticket.domain.ticket.entity.Ticket;
import stack.moaticket.domain.ticket.service.TicketService;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ConcertService concertService;
    private final HallService hallService;
    private final SessionService sessionService;
    private final TicketService ticketService;

    @Transactional
    public CreateConcertDto.Response createConcert(Member member, CreateConcertDto.Request request) {
        if(!member.isSeller()) throw new MoaException(MoaExceptionType.NOT_SELLER);

        Hall hall = hallService.getHallById(request.getHallId());
        int totalSeats = hall.getType().total();

        Concert concert = concertService.createConcert(request.toConcert(member, hall));
        List<Session> sessions = sessionService.insertSessions(concert.getId(), request.getSessions());

        List<Ticket> ticketList = new ArrayList<>();
        List<SessionDto.SessionResponse> sessionResponseList = new ArrayList<>();
        for (Session s : sessions) {
            for(int num = 1; num <= totalSeats; num++) ticketList.add(ticketService.createTicket(s, num));
            sessionResponseList.add(SessionDto.SessionResponse.from(s));
        }
        ticketService.saveAll(ticketList);
        return CreateConcertDto.Response.from(concert, sessionResponseList);
    }

    public ConcertDetailDto.Response getConcertDetail(long concertId) {
        Concert concert = concertService.getConcertById(concertId);
        List<SessionDto.SessionResponse> sessions = sessionService.getSessionsByConcertId(concertId).stream().map(SessionDto.SessionResponse::from).toList();
        ConcertDetailDto.Response response = ConcertDetailDto.Response.from(concert, sessions);
        return response;
    }

    public List<ConcertListDto.Response> getConcertList(String searchValue, String sortBy, String sortOrder, Pageable pageable) {
        List<ConcertListDto.Response> concertList = concertService.getConcertList(searchValue, sortBy, sortOrder, pageable)
                .stream()
                .map(ConcertListDto.Response::from).toList();
        return concertList;
    }

}
