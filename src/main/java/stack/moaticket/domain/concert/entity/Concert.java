package stack.moaticket.domain.concert.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "concert")
public class Concert {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="concert_id")
    private Long id;
}
