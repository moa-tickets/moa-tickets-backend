package stack.moaticket.domain.comment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import stack.moaticket.application.dto.CommentDto;
import stack.moaticket.domain.base.Base;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@SuperBuilder
public class Comment extends Base {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @Column
    private String nickName;

    @Column
    private String content;

    public void fix(CommentDto.CommentFixRequest commentFixRequest) {
        this.nickName = commentFixRequest.nickName();
        this.content = commentFixRequest.content();

    }
}
