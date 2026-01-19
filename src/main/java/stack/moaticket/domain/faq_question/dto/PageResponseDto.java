package stack.moaticket.domain.faq_question.dto;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
public class PageResponseDto<T> {

    private final List<T> contents;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean isFirst;
    private final boolean isLast;

    public PageResponseDto(Page<T> pageData) {
        this.contents = pageData.getContent();
        this.page = pageData.getNumber();
        this.size = pageData.getSize();
        this.totalElements = pageData.getTotalElements();
        this.totalPages = pageData.getTotalPages();
        this.isFirst = pageData.isFirst();
        this.isLast = pageData.isLast();
    }
}
