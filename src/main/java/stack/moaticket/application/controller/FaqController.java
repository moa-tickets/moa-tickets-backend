package stack.moaticket.application.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import stack.moaticket.domain.faq_question.dto.FaqQuestionRequestDTO;
import stack.moaticket.domain.faq_question.dto.FaqQuestionResponseDTO;
import stack.moaticket.domain.faq_question.service.FaqQuestionService;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.system.common.MessageType;
import stack.moaticket.system.common.ResponseApiDTO;

@RestController
@RequiredArgsConstructor
public class FaqController {

    private final FaqQuestionService faqQuestionService;

    @PostMapping(value= "/api/faq")
    public ResponseApiDTO<FaqQuestionResponseDTO> createFaqQuestion(@AuthenticationPrincipal Member member, @RequestPart("dto") FaqQuestionRequestDTO fqdto, @RequestPart(value = "file", required = false) MultipartFile file) {
        FaqQuestionResponseDTO finalDTO = faqQuestionService.createQuestion(member, fqdto, file);
        return ResponseApiDTO.success(MessageType.CREATE, finalDTO);
    }

    @GetMapping(value= "/api/faq")
    public ResponseApiDTO<Page<FaqQuestionResponseDTO>> readFaqQuestion(@AuthenticationPrincipal Member member, Pageable pageable) {
        Page<FaqQuestionResponseDTO> readFinalDTO = faqQuestionService.readQuestionList(member, pageable);
        return ResponseApiDTO.success(MessageType.RETRIEVE, readFinalDTO);
    }

    @PutMapping(value = "/api/faq/{id}")
    public ResponseApiDTO<FaqQuestionResponseDTO> updateFaqQuestion(
            @PathVariable Long id,
            @RequestPart FaqQuestionRequestDTO rqdto,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        FaqQuestionResponseDTO updateFinalDTO = faqQuestionService.updateQuestion(id, rqdto, file);
        return ResponseApiDTO.success(MessageType.UPDATE, updateFinalDTO);
    }
}
