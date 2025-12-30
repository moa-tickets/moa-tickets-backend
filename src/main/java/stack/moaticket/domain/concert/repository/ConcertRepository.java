package stack.moaticket.domain.concert.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stack.moaticket.domain.concert.entity.Concert;

@Repository
public interface ConcertRepository extends JpaRepository<Concert, Long> {
}
