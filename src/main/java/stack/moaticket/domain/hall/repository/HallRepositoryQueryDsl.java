package stack.moaticket.domain.hall.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import stack.moaticket.domain.hall.entity.Hall;
import static stack.moaticket.domain.hall.entity.QHall.hall;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class HallRepositoryQueryDsl {
    private final JPAQueryFactory jpaQueryFactory;

    public Hall getHall(long id) {
        return jpaQueryFactory.selectFrom(hall)
                .where(hall.id.eq(id))
                .fetchOne();
    }

    public List<Hall> getAllHall() {
        return jpaQueryFactory.selectFrom(hall)
                .orderBy(hall.createdAt.desc())
                .fetch();
    }

    public Hall getHallByName(String name) {
        return jpaQueryFactory.selectFrom(hall)
                .where(hall.name.eq(name))
                .fetchOne();
    }
}
