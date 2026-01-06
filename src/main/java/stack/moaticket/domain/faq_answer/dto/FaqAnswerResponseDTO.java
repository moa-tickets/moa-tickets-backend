package stack.moaticket.domain.faq_answer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import stack.moaticket.domain.faq_answer.entity.FaqAnswer;
import stack.moaticket.domain.faq_question.dto.FaqQuestionResponseDTO;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaqAnswerResponseDTO {
    private Long id;
    private String title;
    private String content;

    public static FaqAnswerResponseDTO fromEntity(FaqAnswer entity) {
        return FaqAnswerResponseDTO.builder()
                .id(entity.getId()).title(entity.getTitle()).content(entity.getContent()).build();
    }
}
