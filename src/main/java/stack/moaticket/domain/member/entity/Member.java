package stack.moaticket.domain.member.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import stack.moaticket.domain.base.Base;
import stack.moaticket.domain.member.type.MemberState;

import java.util.Collection;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@SuperBuilder
@Table(name = "member")
public class Member extends Base {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(name = "member_nickname", nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_state", nullable = false)
    private MemberState state;

    @Column(name = "is_seller", nullable = false)
    private boolean isSeller;

    @Column(name = "member_email", nullable = false, updatable = false)
    private String email;

    public void promoteToSeller() {
        this.isSeller = true;
    }

    public void demoteFromSeller() {
        this.isSeller = false;
    }
}
