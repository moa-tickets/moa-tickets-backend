package stack.moaticket.application.service;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import stack.moaticket.application.dto.OnPublishDto;
import stack.moaticket.domain.session_stream.service.SessionStreamService;
import stack.moaticket.system.exception.MoaException;

import static stack.moaticket.system.exception.MoaExceptionType.*;

@Service
@RequiredArgsConstructor
public class RtmpService {
    private final SessionStreamService sessionStreamService;
    private final WebClient webClient;

    private static final String HTTP_SCHEME = "http";
    private static final String WORKER_HOST = "stream-stream-worker";
    private static final int WORKER_PORT = 8000;
    private static final String STREAM_START_PATH = "/streams/{streamKey}/start";
    private static final String STREAM_TERMINATE_PATH = "/streams/{streamKey}/stop";

    public void onPublish(String streamKey) {
        String playbackId = sessionStreamService.validateAndGetPlaybackId(streamKey);

        if(StringUtils.isEmpty(playbackId)) throw new MoaException(INVALID_STREAM_REQUEST);

        startRequestToStreamWorker(streamKey, playbackId);
    }

    public void onDone(String streamKey) {
        boolean valid = sessionStreamService.isExistLiveSession(streamKey);

        if(!valid) throw new MoaException(INVALID_STREAM_REQUEST);

        terminateRequestToStreamWorker(streamKey);
    }

    private void startRequestToStreamWorker(String streamKey, String playbackId) {
        webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme(HTTP_SCHEME)
                        .host(WORKER_HOST)
                        .port(WORKER_PORT)
                        .path(STREAM_START_PATH)
                        .build(streamKey)
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new OnPublishDto.Request(playbackId))
                .exchangeToMono(response -> {
                    if(response.statusCode().is2xxSuccessful()) {
                        return Mono.empty();
                    }
                    return Mono.error(new MoaException(STREAM_ERROR));
                })
                .block();
    }

    private void terminateRequestToStreamWorker(String streamKey) {
        webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme(HTTP_SCHEME)
                        .host(WORKER_HOST)
                        .port(WORKER_PORT)
                        .path(STREAM_TERMINATE_PATH)
                        .build(streamKey)
                )
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return Mono.empty();
                    }
                    return Mono.error(new MoaException(STREAM_ERROR));
                })
                .block();
    }
}
