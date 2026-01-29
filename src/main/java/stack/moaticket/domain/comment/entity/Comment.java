package stack.moaticket.domain.comment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import stack.moaticket.application.dto.CommentDto;
import stack.moaticket.domain.base.Base;
import stack.moaticket.domain.board.entity.Board;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@SuperBuilder
public class Comment extends Base {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long commentId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "board_id")
    private Board board;

    @Column
    private String content;

    public void fix(CommentDto.CommentFixRequest commentFixRequest) {
        this.content = commentFixRequest.content();

    }
}
