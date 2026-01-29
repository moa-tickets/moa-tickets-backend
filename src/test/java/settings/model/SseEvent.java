package settings.model;

import okio.BufferedSource;

import java.time.Duration;

public class SseEvent {
    private final String event;
    private final String data;

    public SseEvent(String event, String data) {
        this.event = event;
        this.data = data;
    }

    public String getEvent() {
        return event;
    }

    public String getData() {
        return data;
    }

    public static SseEvent readNextSseEvent(BufferedSource source, Duration timeout) throws Exception {
        long deadlineNanos = System.nanoTime() + timeout.toNanos();

        String event = null;
        StringBuilder data = new StringBuilder();

        while(System.nanoTime() < deadlineNanos) {
            String line = source.readUtf8Line();
            if(line == null) {
                Thread.sleep(10);
                continue;
            }

            if(line.isEmpty()) {
                if(event != null || !data.isEmpty()) {
                    return new SseEvent(event, data.toString());
                }
                continue;
            }

            if(line.startsWith("event:")) {
                event = line.substring("event:".length()).trim();
            } else if(line.startsWith("data:")) {
                if(!data.isEmpty()) data.append("\n");
                data.append(line.substring("data:".length()).trim());
            }
        }

        throw new AssertionError("Cannot receive SSE(CONNECT) on time");
    }
}
