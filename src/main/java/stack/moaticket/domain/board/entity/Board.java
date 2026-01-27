package stack.moaticket.domain.board.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import stack.moaticket.application.dto.BoardDto;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@SuperBuilder
@Table(name = "board")
public class Board {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id")
    private Long id;

    @Column
    private String title;

    @Column
    private String content;

    @Column
    private String nickName;

    public void fix(BoardDto.BoardFixRequest boardFixRequest) {
        this.title = boardFixRequest.title();
        this.content = boardFixRequest.content();
        this.nickName = boardFixRequest.nickName();
    }
}
