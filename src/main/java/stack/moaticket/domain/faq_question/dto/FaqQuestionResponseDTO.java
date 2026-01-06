package stack.moaticket.domain.faq_question.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.cglib.core.Local;
import stack.moaticket.domain.base.Base;
import stack.moaticket.domain.faq_question.entity.FaqQuestion;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class FaqQuestionResponseDTO {
    private Long id;
    private String title;
    private String content;
    private String faqType;
    private LocalDateTime createdAt;

    public static FaqQuestionResponseDTO fromEntity(FaqQuestion faqQuestion) {
        return FaqQuestionResponseDTO.builder()
                .id(faqQuestion.getId())
                .title(faqQuestion.getTitle())
                .content(faqQuestion.getContents())
                .faqType(faqQuestion.getFaqType())
                .createdAt(faqQuestion.getCreatedAt())
                .build();
    }
}
