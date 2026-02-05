package stack.moaticket.application.dto;

import lombok.Getter;
import lombok.Builder;

@Getter
@Builder
public class KeywordCountDto {
    private String keyword;
    private Long count;

    public static KeywordCountDto of(String keyword, Long count) {
        return KeywordCountDto.builder()
                .keyword(keyword)
                .count(count)
                .build();
    }
}