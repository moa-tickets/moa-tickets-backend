package stack.moaticket.application.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class FilterUtilTest {
    @Test
    void apiFilterSet_exactMatch_true() {
        assertThat(FilterUtil.checkFilter("/login")).isTrue();
        assertThat(FilterUtil.checkFilter("/health")).isTrue();
    }

    @Test
    void prefixFilterSet_startsWith_true() {
        assertThat(FilterUtil.checkFilter("/swagger-ui/index.html")).isTrue();
        assertThat(FilterUtil.checkFilter("/ws/connect")).isTrue();
        assertThat(FilterUtil.checkFilter("/api/product/detail/123")).isTrue();
    }

    @Test
    void notInSet_false() {
        assertThat(FilterUtil.checkFilter("/api/secure/profile")).isFalse();
    }

}