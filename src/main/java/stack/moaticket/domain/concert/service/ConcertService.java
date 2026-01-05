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


    public Concert getConcertById(long concertId){
        Concert concert = concertRepositoryQueryDsl.getConcert(concertId);

        return concert;
    }
    public List<Concert> getConcertList(String concertName, String sortBy, String sortOrder, Pageable pageable){
        List<Concert> concertList = concertRepositoryQueryDsl.getConcertList(concertName, sortBy, sortOrder, pageable);

        return concertList;
    }

}
