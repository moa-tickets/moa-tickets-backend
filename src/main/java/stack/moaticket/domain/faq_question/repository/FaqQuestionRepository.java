package stack.moaticket.domain.faq_question.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import stack.moaticket.domain.faq_question.entity.FaqQuestion;

public interface FaqQuestionRepository extends JpaRepository<FaqQuestion, Long> {
    boolean existsByTitle(String title);
}
