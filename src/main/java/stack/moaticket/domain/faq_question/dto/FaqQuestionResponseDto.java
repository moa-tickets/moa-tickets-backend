package stack.moaticket.domain.faq_question.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;
import stack.moaticket.domain.faq_question.entity.FaqQuestion;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class FaqQuestionResponseDto {
    private Long id;
    private String title;
    private String content;
    private String faqType;
    private LocalDateTime createdAt;

    public static FaqQuestionResponseDto fromEntity(FaqQuestion faqQuestion) {
        return FaqQuestionResponseDto.builder()
                .id(faqQuestion.getId())
                .title(faqQuestion.getTitle())
                .content(faqQuestion.getContents())
                .faqType(faqQuestion.getFaqType())
                .createdAt(faqQuestion.getCreatedAt())
                .build();
    }
}
