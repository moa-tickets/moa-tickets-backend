package stack.moaticket.domain.concert.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
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

    public List<Concert> getConcertList(String concertName, String sortBy, String sortOrder, Pageable pageable){
        List<Concert> concertList =  jpaQueryFactory
                .selectFrom(concert)
                .where(searchName(concertName))
                .orderBy(getOrder(sortBy, sortOrder))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();


        return concertList;
    }
    private OrderSpecifier<?> getOrder(String sortBy, String sortOrder){
        sortBy = sortBy == null ? "date" : sortBy;
        sortOrder = sortOrder == null ? "desc" : sortOrder;

        if (sortOrder.equals("desc")){
            return concert.start.desc();
        }
        else {
            return concert.start.asc();
        }
    }
    private BooleanExpression searchName(String name){
        return StringUtils.hasText(name) ? concert.name.contains(name) : null;
    }
    //UPDATE

    //DELETE


}
