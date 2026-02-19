package stack.moaticket.domain.sentiment.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import stack.moaticket.application.dto.KeywordCountDto;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

@Repository
public class SentimentKeywordRedisRepository {
    private final StringRedisTemplate template;

    public SentimentKeywordRedisRepository(
            @Qualifier("outerStringRedisTemplate") StringRedisTemplate template) {
        this.template = template;
    }

    public List<KeywordCountDto> findTopKeywordsWithCount(String redisKey, int topN) {
        Set<ZSetOperations.TypedTuple<String>> tuples =
                template.opsForZSet().reverseRangeWithScores(redisKey, 0, topN - 1);

        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }

        return tuples.stream()
                .map(t -> KeywordCountDto.of(
                        t.getValue(),
                        t.getScore() == null ? 0L : t.getScore().longValue()
                ))
                .toList();
    }
}
