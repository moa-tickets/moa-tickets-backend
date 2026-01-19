package stack.moaticket.application.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import stack.moaticket.domain.faq_answer.dto.FaqAnswerRequestDto;
import stack.moaticket.domain.faq_answer.dto.FaqAnswerResponseDto;
import stack.moaticket.domain.faq_answer.service.FaqAnswerService;
import stack.moaticket.domain.faq_question.dto.FaqQuestionRequestDto;
import stack.moaticket.domain.faq_question.dto.FaqQuestionResponseDto;
import stack.moaticket.domain.faq_question.dto.PageResponseDto;
import stack.moaticket.domain.faq_question.service.FaqQuestionService;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.system.common.MessageType;
import stack.moaticket.system.common.ResponseApiDto;

@RestController
@RequiredArgsConstructor
public class FaqController {

    private final FaqQuestionService faqQuestionService;
    private final FaqAnswerService faqAnswerService;

    @PostMapping(value= "/api/inquiry",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseApiDto<FaqQuestionResponseDto> createFaqQuestion(
            @AuthenticationPrincipal Long memberId,
            @RequestPart FaqQuestionRequestDto fqDto) {
        FaqQuestionResponseDto finalDto = faqQuestionService.createQuestion(memberId, fqDto);
        return ResponseApiDto.success(MessageType.CREATE, finalDto);
    }

    @GetMapping(value= "/api/inquiry")
    public ResponseApiDto<PageResponseDto<FaqQuestionResponseDto>> readFaqQuestion(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(required = false, defaultValue = "0", value = "page") int pageNum,
            @RequestParam(required = false, defaultValue = "createdAt", value = "criteria") String criteria) {
        Page<FaqQuestionResponseDto> finalPages = faqQuestionService.readQuestionList(memberId, pageNum, criteria);
        PageResponseDto<FaqQuestionResponseDto> convertedPages = new PageResponseDto<>(finalPages);
        return ResponseApiDto.success(MessageType.RETRIEVE, convertedPages);
    }

    @PutMapping(value = "/api/inquiry/{id}")
    public ResponseApiDto<FaqQuestionResponseDto> updateFaqQuestion(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long id,
            @RequestPart FaqQuestionRequestDto rqDto,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        FaqQuestionResponseDto updateFinalDTO = faqQuestionService.updateQuestion(memberId, id, rqDto, file);
        return ResponseApiDto.success(MessageType.UPDATE, updateFinalDTO);
    }

    @DeleteMapping(value = "/api/inquiry/{id}")
    public ResponseApiDto<FaqQuestionResponseDto> deleteFaqQuestion(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long id) {
        FaqQuestionResponseDto deleteFinalDto = faqQuestionService.deleteQuestion(memberId, id);
        return ResponseApiDto.success(MessageType.DELETE, deleteFinalDto);
    }

    @GetMapping(value = "/api/inquiry/{id}")
    public ResponseApiDto<FaqQuestionResponseDto> getDetailFaqQuestion(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long id) {
        FaqQuestionResponseDto detailFinalDTO = faqQuestionService.getDetailQuestion(memberId, id);
        return ResponseApiDto.success(MessageType.RETRIEVE, detailFinalDTO);
    }

    @PostMapping(value="/api/answer/{id}")
    public ResponseApiDto<FaqAnswerResponseDto> answerToQuestion(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long id,
            @RequestPart(value="dto") FaqAnswerRequestDto dto) {
        FaqAnswerResponseDto answerPost = faqAnswerService.answerToQuestionPost(dto, memberId, id);
        return ResponseApiDto.success(MessageType.CREATE, answerPost);
    }

    @GetMapping(value="/api/answer/{id}")
    public ResponseApiDto<FaqAnswerResponseDto> getAnswerData(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long id) {
        FaqAnswerResponseDto answerData = faqAnswerService.getAnswerData(memberId, id);
        return ResponseApiDto.success(MessageType.RETRIEVE, answerData);
    }

    @PutMapping(value="/api/answer/{id}")
    public ResponseApiDto<FaqAnswerResponseDto> updateAnswerData(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long id,
            @RequestPart(value = "dto") FaqAnswerRequestDto dto) {
        FaqAnswerResponseDto updatedAnswerData = faqAnswerService.updateAnswerData(dto, memberId, id);
        return ResponseApiDto.success(MessageType.UPDATE, updatedAnswerData);
    }

    @DeleteMapping("/api/answer/{id}")
    public ResponseApiDto<FaqAnswerResponseDto> deleteAnswerData(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long id) {
        FaqAnswerResponseDto deletedAnswerData = faqAnswerService.deleteAnswerData(memberId, id);
        return ResponseApiDto.success(MessageType.DELETE, deletedAnswerData);
    }
}
