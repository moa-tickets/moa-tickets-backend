package stack.moaticket.domain.comment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import stack.moaticket.application.dto.CommentDto;
import stack.moaticket.domain.base.Base;
import stack.moaticket.domain.board.entity.Board;
import stack.moaticket.domain.member.entity.Member;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@SuperBuilder
public class Comment extends Base {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column
    private String nickName;

    @Column
    private String content;

    public void fix(CommentDto.CommentFixRequest commentFixRequest) {
        this.nickName = commentFixRequest.nickName();
        this.content = commentFixRequest.content();

    }
}
