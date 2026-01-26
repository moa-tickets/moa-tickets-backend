package stack.moaticket.application.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import stack.moaticket.application.dto.ChattingDto;
import stack.moaticket.application.service.ChattingService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChattingService chatService;

    @GetMapping("/chat/history/{playbackId}")
    public ResponseEntity<List<ChattingDto.Response>> getChatHistory(@PathVariable String playbackId,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        List<ChattingDto.Response> chatHistory = chatService.getChatHistory(playbackId, page, size);

        return ResponseEntity.ok(chatHistory);
    }

}
