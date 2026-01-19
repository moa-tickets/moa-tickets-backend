package stack.moaticket.domain.faq_answer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FaqAnswerRequestDto {
    private Long id;
    private String title;
    private String content;
}
