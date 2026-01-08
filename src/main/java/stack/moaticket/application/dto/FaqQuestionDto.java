package stack.moaticket.application.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import stack.moaticket.domain.faq_question.entity.FaqQuestion;

import java.time.LocalDateTime;

public abstract class FaqQuestionDto {

    private FaqQuestionDto() {
        // Utility class
    }

    @Getter
    @NoArgsConstructor
    public static class CreateRequest {
        private String title;
        private String content;
    }

    @Getter
    @NoArgsConstructor
    public static class UpdateRequest {
        private String title;
        private String content;
    }

    @Getter
    @Builder
    public static class Response {
        private Long questionId;
        private Long memberId;
        private String memberNickname;
        private String title;
        private String content;
        private boolean answered;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private FaqAnswerSimpleResponse answer;

        public static Response from(FaqQuestion question) {
            Response.ResponseBuilder builder = Response.builder()
                    .questionId(question.getId())
                    .memberId(question.getMember().getId())
                    .memberNickname(question.getMember().getNickname())
                    .title(question.getTitle())
                    .content(question.getContent())
                    .answered(question.isAnswered())
                    .createdAt(question.getCreatedAt())
                    .updatedAt(question.getUpdatedAt());

            if (question.getFaqAnswer() != null) {
                builder.answer(FaqAnswerSimpleResponse.from(question.getFaqAnswer()));
            }

            return builder.build();
        }
    }

    @Getter
    @Builder
    public static class SimpleResponse {
        private Long questionId;
        private String title;
        private boolean answered;
        private LocalDateTime createdAt;

        public static SimpleResponse from(FaqQuestion question) {
            return SimpleResponse.builder()
                    .questionId(question.getId())
                    .title(question.getTitle())
                    .answered(question.isAnswered())
                    .createdAt(question.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class FaqAnswerSimpleResponse {
        private Long answerId;
        private String content;
        private LocalDateTime createdAt;

        public static FaqAnswerSimpleResponse from(stack.moaticket.domain.faq_answer.entity.FaqAnswer answer) {
            return FaqAnswerSimpleResponse.builder()
                    .answerId(answer.getId())
                    .content(answer.getContent())
                    .createdAt(answer.getCreatedAt())
                    .build();
        }
    }
}

