package stack.moaticket.application.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SentimentKeywordDto {
    private List<KeywordCountDto> positive;
    private List<KeywordCountDto> negative;

    public static SentimentKeywordDto of(
            List<KeywordCountDto> positive,
            List<KeywordCountDto> negative
    ) {
        return SentimentKeywordDto.builder()
                .positive(positive)
                .negative(negative)
                .build();
    }
}
