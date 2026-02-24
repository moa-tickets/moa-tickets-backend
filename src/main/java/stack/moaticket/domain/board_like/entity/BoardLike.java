package stack.moaticket.domain.board_like.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import stack.moaticket.domain.base.Base;
import stack.moaticket.domain.board.entity.Board;
import stack.moaticket.domain.member.entity.Member;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@SuperBuilder
@Table(
        name = "board_like",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_board_like_member_board",
                        columnNames = {"member_id", "board_id"}
                )
        }
)
public class BoardLike extends Base {

    //어떤 게시글에 눌렀는지
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_like_id")
    private Long id;

    //누가 눌렀는지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board board;
}