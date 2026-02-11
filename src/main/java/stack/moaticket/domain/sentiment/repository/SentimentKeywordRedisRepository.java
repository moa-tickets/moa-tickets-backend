package stack.moaticket.domain.sentiment.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import stack.moaticket.application.dto.KeywordCountDto;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SentimentKeywordRedisRepository {

    private final StringRedisTemplate redisTemplate;

    public List<KeywordCountDto> findTopKeywordsWithCount(String redisKey, int topN) {
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(redisKey, 0, topN - 1);

        System.out.println("[DEBUG] redisKey=" + redisKey);
        System.out.println("[DEBUG] exists=" + Boolean.TRUE.equals(redisTemplate.hasKey(redisKey)));

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

    // temp code
    public String debugWriteAndRead() {
        redisTemplate.opsForValue().set("debug:spring", "ok");
        return redisTemplate.opsForValue().get("debug:spring");
    }

}
