package stack.moaticket.domain.hall.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import stack.moaticket.domain.base.Base;
import stack.moaticket.domain.hall.type.HallState;
import stack.moaticket.domain.hall.type.HallType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@SuperBuilder
@Table(name = "hall")
public class Hall extends Base {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hall_id")
    private Long id;

    @Column(name = "hall_name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "hall_type", nullable = false)
    private HallType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "hall_state", nullable = false)
    private HallState state;
}
