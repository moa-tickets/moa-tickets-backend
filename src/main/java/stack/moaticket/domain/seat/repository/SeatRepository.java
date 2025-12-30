package stack.moaticket.domain.seat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stack.moaticket.domain.seat.entity.Seat;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
}
