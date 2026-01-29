package stack.moaticket.domain.board.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import stack.moaticket.application.dto.BoardDto;
import stack.moaticket.domain.base.Base;
import stack.moaticket.domain.member.entity.Member;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@SuperBuilder
@Table(name = "board")
public class Board extends Base{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column
    private String title;

    @Column
    private String content;

    public void fix(BoardDto.BoardFixRequest boardFixRequest) {
        this.title = boardFixRequest.title();
        this.content = boardFixRequest.content();
    }
}