package stack.moaticket.domain.sentiment.service;

import stack.moaticket.application.dto.AspectKeywordsDto;
import stack.moaticket.application.dto.SentimentKeywordDto;
import stack.moaticket.domain.sentiment.model.Aspect;
import stack.moaticket.domain.sentiment.repository.SentimentKeywordRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SentimentService {

    private final SentimentKeywordRedisRepository redisRepository;

    private static final int TOP_N = 7;

    private static String aspectKey(long concertId, String sentiment, Aspect aspect) {
        return "concert:" + concertId + ":" + sentiment + ":aspect:" + aspect.name();
    }

    private List<AspectKeywordsDto> loadAspectTop(long concertId, String sentiment) {
        return Arrays.stream(Aspect.values())
                .map(aspect -> AspectKeywordsDto.of(
                        aspect.name(),
                        redisRepository.findTopKeywordsWithCount(
                                aspectKey(concertId, sentiment, aspect),
                                TOP_N
                        )
                ))
                .toList();
    }

    public SentimentKeywordDto getTopKeywords(long concertId) {
        return SentimentKeywordDto.of(
                loadAspectTop(concertId, "pos"),
                loadAspectTop(concertId, "neg")
        );
    }
}
