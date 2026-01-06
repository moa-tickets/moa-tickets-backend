package stack.moaticket.domain.faq_answer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import stack.moaticket.domain.faq_answer.entity.FaqAnswer;
import stack.moaticket.domain.faq_question.entity.FaqQuestion;
import stack.moaticket.domain.member.entity.Member;

import java.util.Optional;

public interface FaqAnswerRepository extends JpaRepository<FaqAnswer, Long> {
    boolean existsByMemberAndQuestion(Member member, FaqQuestion question);
    Optional<FaqAnswer> findByQuestion(FaqQuestion question);
}
