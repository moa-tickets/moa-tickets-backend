package stack.moaticket.application.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import stack.moaticket.domain.faq_answer.dto.FaqAnswerRequestDTO;
import stack.moaticket.domain.faq_answer.dto.FaqAnswerResponseDTO;
import stack.moaticket.domain.faq_answer.service.FaqAnswerService;
import stack.moaticket.domain.faq_question.dto.FaqQuestionRequestDTO;
import stack.moaticket.domain.faq_question.dto.FaqQuestionResponseDTO;
import stack.moaticket.domain.faq_question.dto.PageResponseDTO;
import stack.moaticket.domain.faq_question.service.FaqQuestionService;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.system.common.MessageType;
import stack.moaticket.system.common.ResponseApiDTO;

@RestController
@RequiredArgsConstructor
public class FaqController {

    private final FaqQuestionService faqQuestionService;
    private final FaqAnswerService faqAnswerService;

    @PostMapping(value= "/api/inquiry",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseApiDTO<FaqQuestionResponseDTO> createFaqQuestion(@AuthenticationPrincipal Member member, @RequestPart FaqQuestionRequestDTO fqdto) {
        FaqQuestionResponseDTO finalDTO = faqQuestionService.createQuestion(member, fqdto);
        return ResponseApiDTO.success(MessageType.CREATE, finalDTO);
    }

    @GetMapping(value= "/api/inquiry")
    public ResponseApiDTO<PageResponseDTO<FaqQuestionResponseDTO>> readFaqQuestion(@AuthenticationPrincipal Member member, @RequestParam(required = false, defaultValue = "0",value = "page") int pageNum,
                                                                        @RequestParam(required = false, defaultValue = "createdAt", value = "criteria") String criteria) {
        Page<FaqQuestionResponseDTO> finalPages = faqQuestionService.readQuestionList(member, pageNum, criteria);
        PageResponseDTO<FaqQuestionResponseDTO> convertedPages = new PageResponseDTO<>(finalPages);
        return ResponseApiDTO.success(MessageType.RETRIEVE, convertedPages);
    }

    @PutMapping(value = "/api/inquiry/{id}")
    public ResponseApiDTO<FaqQuestionResponseDTO> updateFaqQuestion(
            @AuthenticationPrincipal Member member,
            @PathVariable Long id,
            @RequestPart FaqQuestionRequestDTO rqdto,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        FaqQuestionResponseDTO updateFinalDTO = faqQuestionService.updateQuestion(member, id, rqdto, file);
        return ResponseApiDTO.success(MessageType.UPDATE, updateFinalDTO);
    }

    @DeleteMapping(value = "/api/inquiry/{id}")
    public ResponseApiDTO<FaqQuestionResponseDTO> deleteFaqQuestion(@AuthenticationPrincipal Member member,
                                                                    @PathVariable Long id
                                                                    ) {
        FaqQuestionResponseDTO deleteFinalDTO = faqQuestionService.deleteQuestion(member, id);
        return ResponseApiDTO.success(MessageType.DELETE, deleteFinalDTO);
    }

    @GetMapping(value="/api/inquiry/{id}")
    public ResponseApiDTO<FaqQuestionResponseDTO> getDetailFaqQuestion(@AuthenticationPrincipal Member member,@PathVariable Long id) {
        FaqQuestionResponseDTO detailFinalDTO = faqQuestionService.getDetailQuestion(member,id);
        return ResponseApiDTO.success(MessageType.RETRIEVE, detailFinalDTO);
    }

    @PostMapping(value="/api/answer/{id}")
    public ResponseApiDTO<FaqAnswerResponseDTO> answerToQuestion(@AuthenticationPrincipal Member member, @PathVariable Long id, @RequestPart(value="dto") FaqAnswerRequestDTO dto) {
        FaqAnswerResponseDTO answerPost = faqAnswerService.answerToQuestionPost(dto,member, id);
        return ResponseApiDTO.success(MessageType.CREATE, answerPost);
    }

    @GetMapping(value="/api/answer/{id}")
    public ResponseApiDTO<FaqAnswerResponseDTO> getAnswerData(@AuthenticationPrincipal Member member, @PathVariable Long id) {
        FaqAnswerResponseDTO answerData = faqAnswerService.getAnswerData(member,id);
        return ResponseApiDTO.success(MessageType.RETRIEVE, answerData);
    }

    @PutMapping(value="/api/answer/{id}")
    public ResponseApiDTO<FaqAnswerResponseDTO> updateAnswerData(@AuthenticationPrincipal Member member, @PathVariable Long id, @RequestPart(value = "dto") FaqAnswerRequestDTO dto) {
        FaqAnswerResponseDTO updatedAnswerData = faqAnswerService.updateAnswerData(dto,member,id);
        return ResponseApiDTO.success(MessageType.UPDATE, updatedAnswerData);
    }

    @DeleteMapping("/api/answer/{id}")
    public ResponseApiDTO<FaqAnswerResponseDTO> deleteAnswerData(@AuthenticationPrincipal Member member, @PathVariable Long id) {
        FaqAnswerResponseDTO deletedAnswerData = faqAnswerService.deleteAnswerData(member,id);
        return ResponseApiDTO.success(MessageType.DELETE, deletedAnswerData);
    }
}
