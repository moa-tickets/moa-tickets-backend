package stack.moaticket.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

public abstract class OnPublishDto {
    @Getter
    @AllArgsConstructor
    public static class Request {
        private String playbackId;
    }
}
