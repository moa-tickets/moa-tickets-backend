package stack.moaticket.domain.recomment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import stack.moaticket.application.dto.RecommentDto;
import stack.moaticket.domain.base.Base;
import stack.moaticket.domain.comment.entity.Comment;
import stack.moaticket.domain.member.entity.Member;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@SuperBuilder
@Table(name = "recomment")
public class Recomment extends Base {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recomment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommenter_id")
    private Member recommenter;

    @ManyToOne
    @JoinColumn(name = "commenter_id")
    private Comment commenter;

    @Column
    private String content;

    public void fix(RecommentDto.RecommentFixRequest recommentFixRequest) {
        this.content = recommentFixRequest.content();
    }

}
