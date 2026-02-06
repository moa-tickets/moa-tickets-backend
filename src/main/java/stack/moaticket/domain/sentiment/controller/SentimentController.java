package stack.moaticket.domain.sentiment.controller;

import org.springframework.http.ResponseEntity;
import stack.moaticket.application.dto.SentimentKeywordDto;
import stack.moaticket.domain.sentiment.service.SentimentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/concerts/{concertId}/sentiments")
@RequiredArgsConstructor
public class SentimentController {

    private final SentimentService sentimentService;

    @GetMapping("/keywords")
    public ResponseEntity<SentimentKeywordDto> getSentimentKeywords(@PathVariable("concertId") long concertId) {
        SentimentKeywordDto response = sentimentService.getTopKeywords(concertId);
        return ResponseEntity.ok(response);
    }
}
