package stack.moaticket.domain.faq_question.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FaqQuestionRequestDTO {
    private String title;
    private String content;
    private String option;
}
