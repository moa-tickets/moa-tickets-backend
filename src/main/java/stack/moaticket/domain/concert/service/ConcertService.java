package stack.moaticket.domain.concert.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stack.moaticket.application.dto.ConcertDto;
import stack.moaticket.domain.concert.entity.Concert;
import stack.moaticket.domain.concert.repository.ConcertRepository;
import stack.moaticket.domain.concert.repository.ConcertRepositoryQueryDsl;
import stack.moaticket.domain.hall.entity.Hall;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.hall.repository.HallRepositoryQueryDsl;
import stack.moaticket.domain.member.repository.MemberRepositoryQueryDsl;
import stack.moaticket.domain.session.entity.Session;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConcertService {
    private final ConcertRepository concertRepository;
    private final ConcertRepositoryQueryDsl concertRepositoryQueryDsl;
    private final HallRepositoryQueryDsl hallRepositoryQueryDsl;
    private final MemberRepositoryQueryDsl memberRepositoryQueryDsl;

    //CREATE(ConcertRequestDto를 입력받아 그 값을 가지고 Concert entity를 build하고 Concert를 insert시킴
    //콘서트가 insert되면 session도 request가 들어올텐데 그거에 맞는 session도 만들어지고 만들어지는 순간
    //홀에 맞는 좌석만큼의 Ticket을 생성해야함 >>이건 뭐 배치였나 뭔갈 하라던데
    //멤버유저가 isSeller=true면 create가능)
    public Concert insertConcert(ConcertDto.ConcertRequest requestDto){
        Hall hall = hallRepositoryQueryDsl.getHall(requestDto.getHallId());
        Member member = memberRepositoryQueryDsl.findById(requestDto.getMemberId());
        Concert concert = requestDto.toEntity(hall, member);
        return concertRepository.save(concert);
    }

    public ConcertDto.ConcertDetailResponse getConcertDetail(Concert concert, List<Session> sessions){
        return ConcertDto.ConcertDetailResponse.from(concert, sessions);
    }

    //READ(Concert내의 id 값을 입력받아 그 값을 이용해 ConcertQueryDsl의 getConcertById를 이용해 가지고 온다. 하나의 콘서트를 리턴)
    public Concert getConcertById(long concertId){
        Concert concert = concertRepositoryQueryDsl.getConcert(concertId);

        return concert;
    }
    //READ(검색조건에 맞춰서 여러개 생성 예정)
    public List<ConcertDto.ConcertResponse> getConcerts(){
        List<Concert> concerts = concertRepositoryQueryDsl.getConcerts();

        return concerts.stream()
                .map(ConcertDto.ConcertResponse::from)
                .toList();
    }

    //UPDATE(ConcertDto를 입력받아 그 안의 id와 내용을 가지고 update실행
    public long updateConcert(long id, ConcertDto.ConcertRequest request){
        Concert concert = concertRepositoryQueryDsl.getConcert(id);

        Hall hall = hallRepositoryQueryDsl.getHall(request.getHallId());

        concert.updateConcert(request, hall);
        return concert.getId();
    }
}
