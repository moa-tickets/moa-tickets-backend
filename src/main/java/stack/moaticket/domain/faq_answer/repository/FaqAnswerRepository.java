package stack.moaticket.domain.faq_answer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import stack.moaticket.domain.faq_answer.entity.FaqAnswer;
import stack.moaticket.domain.faq_question.entity.FaqQuestion;

import java.util.Optional;

public interface FaqAnswerRepository extends JpaRepository<FaqAnswer, Long> {
    Optional<FaqAnswer> findByQuestion(FaqQuestion question);
}
