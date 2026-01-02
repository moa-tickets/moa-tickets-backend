package stack.moaticket.domain.session_stream.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stack.moaticket.domain.session_stream.entity.SessionStream;

@Repository
public interface SessionStreamRepository extends JpaRepository<SessionStream, Long> {
}
