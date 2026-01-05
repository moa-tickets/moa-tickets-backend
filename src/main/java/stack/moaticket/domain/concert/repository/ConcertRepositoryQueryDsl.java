package stack.moaticket.domain.concert.repository;

import com.querydsl.core.QueryFactory;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import stack.moaticket.domain.concert.entity.Concert;

import java.util.List;

import static stack.moaticket.domain.concert.entity.QConcert.concert;
import static stack.moaticket.domain.hall.entity.QHall.hall;
import static stack.moaticket.domain.member.entity.QMember.member;

@Repository
@RequiredArgsConstructor
public class ConcertRepositoryQueryDsl {
    private final JPAQueryFactory jpaQueryFactory;


    //READ
    public Concert getConcert(long id){
        return jpaQueryFactory.selectFrom(concert)
                .join(concert.member, member).fetchJoin()
                .join(concert.hall, hall).fetchJoin()
                .where(concert.id.eq(id))
                .fetchOne();
    }

    public List<Concert> getConcerts(){
        return jpaQueryFactory
                .selectFrom(concert)
                .join(concert.member, member).fetchJoin()
                .join(concert.hall, hall).fetchJoin()
                .fetch();
    }

    public List<Concert> getConcertList(String concerName, String sortBy, String sortOrder, Pageable pageable){
        return jpaQueryFactory
                .selectFrom(concert)
                .fetch();
    }

    //UPDATE

    //DELETE


}
