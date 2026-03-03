package stack.moaticket.domain.session.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stack.moaticket.domain.session.entity.Session;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
}
