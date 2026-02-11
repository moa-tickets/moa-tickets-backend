package stack.moaticket.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AspectKeywordsDto {
    private String aspect;
    private List<KeywordCountDto> keywords;

    public static AspectKeywordsDto of(String aspect, List<KeywordCountDto> keywords) {
        return AspectKeywordsDto.builder()
                .aspect(aspect)
                .keywords(keywords)
                .build();
    }
}
