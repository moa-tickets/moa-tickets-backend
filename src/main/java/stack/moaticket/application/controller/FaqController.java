package stack.moaticket.application.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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

    @PostMapping(value= "/api/inquiry")
    public ResponseApiDTO<FaqQuestionResponseDTO> createFaqQuestion(@AuthenticationPrincipal Member member, @RequestPart("dto") FaqQuestionRequestDTO fqdto, @RequestPart(value = "file", required = false) MultipartFile file) {
        FaqQuestionResponseDTO finalDTO = faqQuestionService.createQuestion(member, fqdto, file);
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

}
