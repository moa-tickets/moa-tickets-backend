package stack.moaticket.domain.member.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "member")
public class Member {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="member_id")
    private Long id;
}
