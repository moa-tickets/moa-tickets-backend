package stack.moaticket.domain.sentiment.service;

import stack.moaticket.application.dto.SentimentKeywordDto;
import stack.moaticket.domain.sentiment.repository.SentimentKeywordRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SentimentService {

    private final SentimentKeywordRedisRepository redisRepository;

    private static final String POSITIVE_KEY = "positive_keywords";
    private static final String NEGATIVE_KEY = "negative_keywords";
    private static final int TOP_N = 7;

    public SentimentKeywordDto getTopKeywords() {
        return SentimentKeywordDto.of(
                redisRepository.findTopKeywordsWithCount(POSITIVE_KEY, TOP_N),
                redisRepository.findTopKeywordsWithCount(NEGATIVE_KEY, TOP_N)
        );
    }
}
