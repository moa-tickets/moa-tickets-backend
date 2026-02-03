package stack.moaticket.domain.concert.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import stack.moaticket.domain.base.Base;
import stack.moaticket.domain.hall.entity.Hall;
import stack.moaticket.domain.member.entity.Member;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@SuperBuilder
@Table(name = "concert")
public class Concert extends Base {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "concert_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "hall_id", nullable = false)
    private Hall hall;

    @Column(name = "concert_name", nullable = false)
    private String name;

    @Column(name = "concert_duration", nullable = false)
    private String duration;

    @Column(name = "concert_age", nullable = false)
    private int age;

    @Column(name = "concert_booking_open", nullable = false)
    private LocalDateTime bookingOpen;

    @Column(name = "concert_start", nullable = false)
    private LocalDateTime start;

    @Column(name = "concert_end", nullable = false)
    private LocalDateTime end;

    @Column(name = "concert_thumbnail", nullable = true)
    private String thumbnail;

    @Column(name = "concert_detail", nullable = true)
    private String detail;


}
