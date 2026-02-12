package stack.moaticket.domain.recomment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stack.moaticket.domain.recomment.entity.Recomment;

@Repository
public interface RecommentRepository extends JpaRepository<Recomment, Long> {
}
