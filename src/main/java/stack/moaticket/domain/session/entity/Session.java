package stack.moaticket.domain.session.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import stack.moaticket.domain.base.Base;
import stack.moaticket.domain.concert.entity.Concert;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@SuperBuilder
@Table(name = "session",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_concert_session_date",
                        columnNames = {"concert_id", "session_date"}
                )
        }
)
public class Session extends Base {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    @Column(name = "session_date", nullable = false)
    private LocalDateTime date;

    @Column(name = "session_price")
    private int price;
}
