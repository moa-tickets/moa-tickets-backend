package stack.moaticket.domain.hall.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stack.moaticket.domain.hall.entity.Hall;

@Repository
public interface HallRepository extends JpaRepository<Hall, Long> {
}
