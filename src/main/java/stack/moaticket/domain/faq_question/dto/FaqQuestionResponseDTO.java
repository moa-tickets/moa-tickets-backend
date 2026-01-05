package stack.moaticket.domain.faq_question.dto;

import lombok.*;
import stack.moaticket.domain.faq_question.entity.FaqQuestion;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaqQuestionResponseDTO {
    private Long id;
    private String title;
    private String content;
    private String faqType;

    public static FaqQuestionResponseDTO fromEntity(FaqQuestion faqQuestion) {
        return FaqQuestionResponseDTO.builder()
                .id(faqQuestion.getId())
                .title(faqQuestion.getTitle())
                .content(faqQuestion.getContents())
                .faqType(faqQuestion.getFaqType())
                .build();
    }
}
