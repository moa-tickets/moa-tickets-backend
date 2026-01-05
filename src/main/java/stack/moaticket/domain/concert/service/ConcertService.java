package stack.moaticket.domain.concert.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.application.dto.ConcertListDto;
import stack.moaticket.application.dto.CreateConcertDto;
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

    public Concert createConcert(Concert concert){
        return concertRepository.save(concert);
    }


    //READ(Concert내의 id 값을 입력받아 그 값을 이용해 ConcertQueryDsl의 getConcertById를 이용해 가지고 온다. 하나의 콘서트를 리턴)
    public Concert getConcertById(long concertId){
        Concert concert = concertRepositoryQueryDsl.getConcert(concertId);

        return concert;
    }
    //READ(검색조건에 맞춰서 여러개 생성 예정)
    public List<Concert> getConcertList(String concertName, String sortBy, String sortOrder, Pageable pageable){
        List<Concert> concertList = concertRepositoryQueryDsl.getConcertList(concertName, sortBy, sortOrder, pageable);

        return concertList;
    }

    //UPDATE(ConcertDto를 입력받아 그 안의 id와 내용을 가지고 update실행
//    @Transactional
//    public long updateConcert(long id, CreateConcertDto.Request request){
//        Concert concert = concertRepositoryQueryDsl.getConcert(id);
//
//        Hall hall = hallRepositoryQueryDsl.getHall(request.getHallId());
//
//        concert.updateConcert(request, hall);
//        return concert.getId();
//    }
}
