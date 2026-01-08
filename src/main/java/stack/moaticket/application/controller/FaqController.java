package stack.moaticket.application.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import stack.moaticket.application.dto.FaqAnswerDto;
import stack.moaticket.application.dto.FaqQuestionDto;
import stack.moaticket.domain.faq_answer.service.FaqAnswerService;
import stack.moaticket.domain.faq_question.service.FaqQuestionService;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

@RestController
@RequestMapping("/api/faq")
@RequiredArgsConstructor
public class FaqController {

    private final FaqQuestionService faqQuestionService;
    private final FaqAnswerService faqAnswerService;

    // ========== FAQ Question ==========

    /**
     * FAQ 질문 생성
     */
    @PostMapping("/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<FaqQuestionDto.Response> createQuestion(
            @RequestBody FaqQuestionDto.CreateRequest request,
            @AuthenticationPrincipal Member member
    ) {
        if (member == null) {
            throw new MoaException(MoaExceptionType.UNAUTHORIZED);
        }
        FaqQuestionDto.Response response = faqQuestionService.createQuestion(request, member.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * FAQ 질문 목록 조회 (페이지네이션)
     */
    @GetMapping("/questions")
    public ResponseEntity<Page<FaqQuestionDto.SimpleResponse>> getAllQuestions(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        Page<FaqQuestionDto.SimpleResponse> response = faqQuestionService.getAllQuestions(pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * FAQ 질문 상세 조회
     */
    @GetMapping("/questions/{questionId}")
    public ResponseEntity<FaqQuestionDto.Response> getQuestionById(
            @PathVariable Long questionId
    ) {
        FaqQuestionDto.Response response = faqQuestionService.getQuestionById(questionId);
        return ResponseEntity.ok(response);
    }

    /**
     * FAQ 질문 수정
     */
    @PutMapping("/questions/{questionId}")
    public ResponseEntity<FaqQuestionDto.Response> updateQuestion(
            @PathVariable Long questionId,
            @RequestBody FaqQuestionDto.UpdateRequest request,
            @AuthenticationPrincipal Member member
    ) {
        if (member == null) {
            throw new MoaException(MoaExceptionType.UNAUTHORIZED);
        }
        FaqQuestionDto.Response response = faqQuestionService.updateQuestion(questionId, request, member.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * FAQ 질문 삭제
     */
    @DeleteMapping("/questions/{questionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteQuestion(
            @PathVariable Long questionId,
            @AuthenticationPrincipal Member member
    ) {
        if (member == null) {
            throw new MoaException(MoaExceptionType.UNAUTHORIZED);
        }
        faqQuestionService.deleteQuestion(questionId, member.getId());
        return ResponseEntity.noContent().build();
    }

    // ========== FAQ Answer ==========

    /**
     * FAQ 답변 생성
     */
    @PostMapping("/answers")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<FaqAnswerDto.Response> createAnswer(
            @RequestBody FaqAnswerDto.CreateRequest request,
            @AuthenticationPrincipal Member member
    ) {
        if (member == null) {
            throw new MoaException(MoaExceptionType.UNAUTHORIZED);
        }
        FaqAnswerDto.Response response = faqAnswerService.createAnswer(request, member.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * FAQ 답변 조회
     */
    @GetMapping("/answers/{answerId}")
    public ResponseEntity<FaqAnswerDto.Response> getAnswerById(
            @PathVariable Long answerId
    ) {
        FaqAnswerDto.Response response = faqAnswerService.getAnswerById(answerId);
        return ResponseEntity.ok(response);
    }

    /**
     * FAQ 답변 수정
     */
    @PutMapping("/answers/{answerId}")
    public ResponseEntity<FaqAnswerDto.Response> updateAnswer(
            @PathVariable Long answerId,
            @RequestBody FaqAnswerDto.UpdateRequest request,
            @AuthenticationPrincipal Member member
    ) {
        if (member == null) {
            throw new MoaException(MoaExceptionType.UNAUTHORIZED);
        }
        FaqAnswerDto.Response response = faqAnswerService.updateAnswer(answerId, request, member.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * FAQ 답변 삭제
     */
    @DeleteMapping("/answers/{answerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteAnswer(
            @PathVariable Long answerId,
            @AuthenticationPrincipal Member member
    ) {
        if (member == null) {
            throw new MoaException(MoaExceptionType.UNAUTHORIZED);
        }
        faqAnswerService.deleteAnswer(answerId, member.getId());
        return ResponseEntity.noContent().build();
    }
}
