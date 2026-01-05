package stack.moaticket.application.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import stack.moaticket.application.service.RtmpService;

@RestController
@RequestMapping("/rtmp")
@RequiredArgsConstructor
public class RtmpController {
    private final RtmpService rtmpService;

    @PostMapping("/on-publish")
    public ResponseEntity<Void> verifyOnPublish(
            @RequestParam("name") String streamKey) {
        rtmpService.onPublish(streamKey);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/on-done")
    public ResponseEntity<Void> verifyOnDone(
            @RequestParam("name") String streamKey) {
        rtmpService.onDone(streamKey);
        return ResponseEntity.noContent().build();
    }
}
