package stack.moaticket.application.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import stack.moaticket.domain.faq_answer.entity.FaqAnswer;

import java.time.LocalDateTime;

public abstract class FaqAnswerDto {

    private FaqAnswerDto() {
        // Utility class
    }

    @Getter
    @NoArgsConstructor
    public static class CreateRequest {
        private Long questionId;
        private String content;
    }

    @Getter
    @NoArgsConstructor
    public static class UpdateRequest {
        private String content;
    }

    @Getter
    @Builder
    public static class Response {
        private Long answerId;
        private Long questionId;
        private Long memberId;
        private String memberNickname;
        private String content;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(FaqAnswer answer) {
            return Response.builder()
                    .answerId(answer.getId())
                    .questionId(answer.getQuestion().getId())
                    .memberId(answer.getMember().getId())
                    .memberNickname(answer.getMember().getNickname())
                    .content(answer.getContent())
                    .createdAt(answer.getCreatedAt())
                    .updatedAt(answer.getUpdatedAt())
                    .build();
        }
    }
}

