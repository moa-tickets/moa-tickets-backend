package stack.moaticket.domain.board_like.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stack.moaticket.domain.board_like.entity.BoardLike;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardLikeRepository extends JpaRepository<BoardLike, Long> {
    List<BoardLike> findByMemberId(Long memberId);

    Optional<BoardLike> findByMemberIdAndBoardId(Long memberId, Long boardId);

    boolean existsByBoardIdAndMemberId(Long boardId, Long memberId);

    void deleteByBoardIdAndMemberId(Long boardId, Long memberId);
}

