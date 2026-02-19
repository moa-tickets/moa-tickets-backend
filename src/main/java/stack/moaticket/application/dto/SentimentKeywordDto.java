package stack.moaticket.application.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SentimentKeywordDto {
    private List<AspectKeywordsDto> positive;
    private List<AspectKeywordsDto> negative;

    public static SentimentKeywordDto of(
            List<AspectKeywordsDto> positive,
            List<AspectKeywordsDto> negative
    ) {
        return SentimentKeywordDto.builder()
                .positive(positive)
                .negative(negative)
                .build();
    }
}
