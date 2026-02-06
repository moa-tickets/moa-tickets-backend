package stack.moaticket.domain.sentiment.service;

import stack.moaticket.application.dto.SentimentKeywordDto;
import stack.moaticket.domain.sentiment.repository.SentimentKeywordRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SentimentService {

    private final SentimentKeywordRedisRepository redisRepository;

    private static final int TOP_N = 7;

    private static String positiveKey(long concertId) {
        return "concert:" + concertId + ":positive_keywords";
    }

    private static String negativeKey(long concertId) {
        return "concert:" + concertId + ":negative_keywords";
    }

    public SentimentKeywordDto getTopKeywords(long concertId) {
        return SentimentKeywordDto.of(
                redisRepository.findTopKeywordsWithCount(positiveKey(concertId), TOP_N),
                redisRepository.findTopKeywordsWithCount(negativeKey(concertId), TOP_N)
        );
    }
}
