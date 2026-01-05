package stack.moaticket.application.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import stack.moaticket.domain.faq_question.dto.FaqQuestionRequestDTO;
import stack.moaticket.domain.faq_question.dto.FaqQuestionResponseDTO;
import stack.moaticket.domain.faq_question.service.FaqQuestionService;
import stack.moaticket.system.common.MessageType;
import stack.moaticket.system.common.ResponseApiDTO;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FaqController {

    private final FaqQuestionService faqQuestionService;

    @PostMapping(value= "/api/faq/create")
    public ResponseApiDTO<FaqQuestionResponseDTO> createFaqQuestion(@RequestPart FaqQuestionRequestDTO rdto, @RequestPart(value = "file", required = false) MultipartFile file) {
        FaqQuestionResponseDTO finalDTO = faqQuestionService.createQuestion(rdto, file);
        return ResponseApiDTO.success(MessageType.CREATE, finalDTO);
    }

    @GetMapping(value= "/api/faq/read")
    public ResponseApiDTO<List<FaqQuestionResponseDTO>> readFaqQuestion() {
        List<FaqQuestionResponseDTO> readFinalDTO = faqQuestionService.readQuestionList();
        return ResponseApiDTO.success(MessageType.RETRIEVE, readFinalDTO);
    }

    @PutMapping(value = "/api/faq/{id}/update")
    public ResponseApiDTO<FaqQuestionResponseDTO> updateFaqQuestion(@PathVariable Long id, @RequestPart FaqQuestionRequestDTO rqdto, @RequestPart(value = "file", required = false) MultipartFile file) {
        FaqQuestionResponseDTO updateFinalDTO = faqQuestionService.updateQuestion(id, rqdto, file);
        return ResponseApiDTO.success(MessageType.UPDATE, updateFinalDTO);
    }
}
