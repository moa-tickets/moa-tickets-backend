package stack.moaticket.domain.faq_answer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import stack.moaticket.domain.faq_answer.entity.FaqAnswer;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaqAnswerResponseDto {
    private Long id;
    private String title;
    private String content;

    public static FaqAnswerResponseDto fromEntity(FaqAnswer entity) {
        return FaqAnswerResponseDto.builder()
                .id(entity.getId()).title(entity.getTitle()).content(entity.getContent()).build();
    }
}
