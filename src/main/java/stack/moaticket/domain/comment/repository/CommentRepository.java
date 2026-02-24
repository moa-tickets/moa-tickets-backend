package stack.moaticket.domain.comment.repository;

import com.querydsl.core.Fetchable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stack.moaticket.domain.comment.entity.Comment;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

}