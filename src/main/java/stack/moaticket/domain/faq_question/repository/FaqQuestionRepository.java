package stack.moaticket.domain.faq_question.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import stack.moaticket.domain.faq_question.entity.FaqQuestion;

import java.util.List;

public interface FaqQuestionRepository extends JpaRepository<FaqQuestion, Long> {
    boolean existsByTitle(String title);

    @Override
    List<FaqQuestion> findAll();
    Page<FaqQuestion> findAll(Pageable pageable);



}
